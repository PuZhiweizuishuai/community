package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.AdminCommentDetailDto;
import com.buguagaoshu.community.dto.AdminCommentDto;
import com.buguagaoshu.community.dto.AdminCommentQuestionDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.CommentTypeEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdminCommentService;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.CacheInvalidation;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.buguagaoshu.community.model.table.CommentTableDef.COMMENT;
import static com.buguagaoshu.community.model.table.QuestionTableDef.QUESTION;

/**
 * 后台评论管理实现。
 *
 * <p>删除状态与审核状态相互独立：恢复评论只还原 status，是否公开展示仍由 reviewStatus 决定；
 * 只有曾通过审核的评论才计入问题/根评论回复数，删除与恢复时同步做原子增减。
 */
@Service
@Slf4j
public class AdminCommentServiceImpl implements AdminCommentService {

    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_DELETED = 0;

    private final CommentMapper commentMapper;
    private final QuestionMapper questionMapper;
    private final UserBatchLoader userBatchLoader;
    private final NotificationService notificationService;
    private final SiteCacheService siteCache;

    public AdminCommentServiceImpl(CommentMapper commentMapper,
                                   QuestionMapper questionMapper,
                                   UserBatchLoader userBatchLoader,
                                   NotificationService notificationService,
                                   SiteCacheService siteCache) {
        this.commentMapper = commentMapper;
        this.questionMapper = questionMapper;
        this.userBatchLoader = userBatchLoader;
        this.notificationService = notificationService;
        this.siteCache = siteCache;
    }

    @Override
    public PaginationDto<AdminCommentDto> listForAdmin(String status, String keyword, String page, String size) {
        Integer statusFilter = parseStatus(status);
        String search = StringUtil.sanitizeSearchKeyword(keyword);
        long count = commentMapper.selectCountByQuery(listQuery(statusFilter, search));
        long[] param = NumberUtils.getPageAndSize(page, size, count);
        List<Comment> comments = commentMapper.selectListByQuery(
                listQuery(statusFilter, search)
                        .orderBy(COMMENT.COMMENT_ID.desc())
                        .limit(param[0], param[1]));

        Map<Long, Question> questionMap = loadQuestions(comments);
        Map<Long, Comment> parentMap = loadParents(comments);
        Map<Long, User> userMap = loadUsers(comments, questionMap, parentMap);
        List<AdminCommentDto> data = comments.stream()
                .map(comment -> toDto(comment, questionMap.get(comment.getQuestionId()), userMap, parentMap))
                .toList();

        PaginationDto<AdminCommentDto> pagination = new PaginationDto<>();
        pagination.setData(data);
        pagination.setAllCount(count);
        pagination.setPagination(param[2], param[3], param[1]);
        return pagination;
    }

    @Override
    public AdminCommentDetailDto detail(long commentId) {
        Comment comment = commentMapper.selectOneById(commentId);
        if (comment == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "评论不存在或已被彻底删除");
        }
        Question question = questionMapper.selectOneById(comment.getQuestionId());
        Comment parent = null;
        Comment root = null;
        if (comment.getType() == CommentTypeEnum.COMMENT.getType()) {
            if (comment.getParentId() > 0) {
                parent = commentMapper.selectOneById(comment.getParentId());
            }
            if (comment.getParentCommentId() > 0 && comment.getParentCommentId() != comment.getParentId()) {
                root = commentMapper.selectOneById(comment.getParentCommentId());
            }
        }

        Set<Long> userIds = new HashSet<>();
        addUser(userIds, comment);
        addUser(userIds, parent);
        addUser(userIds, root);
        if (question != null) {
            userIds.add(question.getUserId());
        }
        Map<Long, User> userMap = userBatchLoader.loadUsers(userIds);

        Map<Long, Comment> parentMap = new HashMap<>(1);
        if (parent != null) {
            parentMap.put(parent.getCommentId(), parent);
        }

        AdminCommentDetailDto detail = new AdminCommentDetailDto();
        detail.setComment(toDto(comment, question, userMap, parentMap));
        detail.setQuestion(toQuestionDto(question, userMap));
        if (parent != null) {
            detail.setParentComment(toDto(parent, question, userMap, null));
        }
        if (root != null) {
            detail.setRootComment(toDto(root, question, userMap, null));
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long commentId, User admin) {
        Comment comment = commentMapper.selectOneById(commentId);
        if (comment == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "评论不存在或已被彻底删除");
        }
        if (comment.getStatus() == STATUS_DELETED) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "该评论已处于删除状态");
        }
        if (commentMapper.alterStatus(commentId, STATUS_DELETED, STATUS_NORMAL) != 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "评论状态已变化，请刷新后重试");
        }
        if (comment.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
            adjustCommentCount(comment, -1);
        }
        notifyAuthor(comment, admin, NotificationTypeEnum.ADMIN_DELETE_COMMENT,
                "你的评论已被管理员删除：" + StringUtil.subString(safeContent(comment), 100));
        CacheInvalidation.afterCommit(() -> siteCache.invalidate(SiteCacheService.HOME));
        log.info("管理员 {} 删除评论 {}", admin.getId(), commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recover(long commentId, User admin) {
        Comment comment = commentMapper.selectOneById(commentId);
        if (comment == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "评论不存在或已被彻底删除");
        }
        if (comment.getStatus() == STATUS_NORMAL) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "该评论未被删除");
        }
        if (commentMapper.alterStatus(commentId, STATUS_NORMAL, STATUS_DELETED) != 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "评论状态已变化，请刷新后重试");
        }
        if (comment.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
            adjustCommentCount(comment, 1);
        }
        notifyAuthor(comment, admin, NotificationTypeEnum.ADMIN_RECOVER_COMMENT,
                "你的评论已被管理员恢复：" + StringUtil.subString(safeContent(comment), 100));
        CacheInvalidation.afterCommit(() -> siteCache.invalidate(SiteCacheService.HOME));
        log.info("管理员 {} 恢复评论 {}", admin.getId(), commentId);
    }

    private Integer parseStatus(String status) {
        if (StringUtils.isBlank(status) || "-1".equals(status.trim())) {
            return null;
        }
        int value;
        try {
            value = Integer.parseInt(status.trim());
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        if (value != STATUS_DELETED && value != STATUS_NORMAL) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        return value;
    }

    private QueryWrapper listQuery(Integer status, String keyword) {
        QueryWrapper query = QueryWrapper.create();
        if (status != null) {
            query.and(COMMENT.STATUS.eq(status));
        }
        if (keyword != null) {
            query.and(COMMENT.CONTENT.like(keyword));
        }
        return query;
    }

    private Map<Long, Question> loadQuestions(List<Comment> comments) {
        Set<Long> questionIds = comments.stream()
                .map(Comment::getQuestionId)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionMapper.selectListByQuery(QueryWrapper.create()
                        .select(QUESTION.QUESTION_ID, QUESTION.TITLE, QUESTION.STATUS, QUESTION.REVIEW_STATUS,
                                QUESTION.USER_ID, QUESTION.CLASSIFICATION, QUESTION.TAG, QUESTION.CREATE_TIME)
                        .where(QUESTION.QUESTION_ID.in(questionIds)))
                .stream().collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
    }

    private Map<Long, Comment> loadParents(List<Comment> comments) {
        Set<Long> parentIds = comments.stream()
                .filter(comment -> comment.getType() == CommentTypeEnum.COMMENT.getType())
                .map(Comment::getParentId)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return commentMapper.selectListByIds(parentIds).stream()
                .collect(Collectors.toMap(Comment::getCommentId, Function.identity()));
    }

    private Map<Long, User> loadUsers(List<Comment> comments, Map<Long, Question> questionMap,
                                      Map<Long, Comment> parentMap) {
        Set<Long> userIds = new HashSet<>();
        comments.forEach(comment -> userIds.add(comment.getCommentator()));
        parentMap.values().forEach(comment -> userIds.add(comment.getCommentator()));
        questionMap.values().forEach(question -> userIds.add(question.getUserId()));
        return userBatchLoader.loadUsers(userIds);
    }

    private AdminCommentDto toDto(Comment comment, Question question, Map<Long, User> userMap,
                                  Map<Long, Comment> parentMap) {
        AdminCommentDto dto = new AdminCommentDto();
        BeanUtils.copyProperties(comment, dto);
        dto.setAuthorId(comment.getCommentator());
        fillAuthor(dto, userMap.get(comment.getCommentator()));
        if (question == null) {
            dto.setQuestionTitle("问题不存在");
        } else {
            dto.setQuestionTitle(question.getTitle());
            dto.setQuestionStatus(question.getStatus());
            dto.setQuestionReviewStatus(question.getReviewStatus());
        }
        if (comment.getType() == CommentTypeEnum.COMMENT.getType() && parentMap != null) {
            Comment parent = parentMap.get(comment.getParentId());
            if (parent == null) {
                dto.setParentAuthorName("已删除的评论");
            } else {
                User parentAuthor = userMap.get(parent.getCommentator());
                dto.setParentAuthorName(parentAuthor == null ? "未知用户" : parentAuthor.getUserName());
            }
        }
        dto.setCreateTime(formatTime(comment.getCreateTime()));
        dto.setModifiedTime(formatTime(comment.getModifiedTime()));
        return dto;
    }

    private AdminCommentQuestionDto toQuestionDto(Question question, Map<Long, User> userMap) {
        if (question == null) {
            return null;
        }
        AdminCommentQuestionDto dto = new AdminCommentQuestionDto();
        dto.setQuestionId(question.getQuestionId());
        dto.setTitle(question.getTitle());
        dto.setClassification(question.getClassification());
        dto.setTag(question.getTag());
        dto.setStatus(question.getStatus());
        dto.setReviewStatus(question.getReviewStatus());
        dto.setAuthorId(question.getUserId());
        User author = userMap.get(question.getUserId());
        if (author != null) {
            dto.setAuthorUserId(author.getUserId());
            dto.setAuthorName(author.getUserName());
        }
        dto.setCreateTime(formatTime(question.getCreateTime()));
        return dto;
    }

    private void fillAuthor(AdminCommentDto dto, User user) {
        if (user == null) {
            dto.setAuthorName("未知用户");
            return;
        }
        dto.setAuthorUserId(user.getUserId());
        dto.setAuthorName(user.getUserName());
        dto.setAuthorHeadUrl(user.getHeadUrl());
    }

    /**
     * 评论计数只对「已通过审核」的评论生效：
     * 问题回复数始终增减，二级回复同时维护根评论的回复数。
     */
    private void adjustCommentCount(Comment comment, int delta) {
        Question questionUpdate = new Question();
        questionUpdate.setQuestionId(comment.getQuestionId());
        questionUpdate.setCommentCount(delta);
        questionMapper.updateQuestionCommentCount(questionUpdate);

        if (comment.getType() == CommentTypeEnum.COMMENT.getType() && comment.getParentCommentId() > 0) {
            Comment rootUpdate = new Comment();
            rootUpdate.setCommentId(comment.getParentCommentId());
            rootUpdate.setCommentCount(delta);
            commentMapper.updateCommentCount(rootUpdate);
        }
    }

    private void notifyAuthor(Comment comment, User admin, NotificationTypeEnum type, String content) {
        if (comment.getCommentator() == admin.getId()) {
            return;
        }
        notificationService.createSystemNotification(comment.getCommentator(), admin.getId(),
                comment.getQuestionId(), comment.getCommentId(), type.getType(), content);
    }

    private void addUser(Set<Long> userIds, Comment comment) {
        if (comment != null && comment.getCommentator() > 0) {
            userIds.add(comment.getCommentator());
        }
    }

    private String safeContent(Comment comment) {
        return comment.getContent() == null ? "" : comment.getContent();
    }

    private String formatTime(long time) {
        return time > 0 ? StringUtil.foematTime(time) : "";
    }
}
