package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ReviewDetailDto;
import com.buguagaoshu.community.dto.ReviewItemDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.ReviewActionEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.enums.ReviewTargetTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.ContentReviewLogMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.ContentReviewLog;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionContentService;
import com.buguagaoshu.community.service.ReviewService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.util.CacheInvalidation;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.buguagaoshu.community.model.table.CommentTableDef.COMMENT;
import static com.buguagaoshu.community.model.table.QuestionTableDef.QUESTION;

/**
 * 内容审核实现。
 *
 * <p>关键约束：
 * <ul>
 *   <li>审核状态与删除状态独立，公开查询统一要求 reviewStatus = 1；</li>
 *   <li>话题讨论数、评论计数、回复通知只在审核通过后生效，避免待审内容影响站内统计；</li>
 *   <li>审核通过/驳回使用带 reviewStatus = 0 条件的原子更新，并发审核只有一个请求生效；</li>
 *   <li>每次提交/通过/驳回都写入 content_review_log，保证审核过程可追溯。</li>
 * </ul>
 */
@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_REASON_LENGTH = 500;
    private static final int MAX_SUGGESTION_LENGTH = 500;
    private static final int MAX_COMMENT_LENGTH = 5000;

    private final QuestionMapper questionMapper;
    private final CommentMapper commentMapper;
    private final ContentReviewLogMapper reviewLogMapper;
    private final CommentService commentService;
    private final TagClassService tagClassService;
    private final NotificationService notificationService;
    private final QuestionContentService questionContentService;
    private final UserBatchLoader userBatchLoader;
    private final SiteCacheService siteCache;

    public ReviewServiceImpl(QuestionMapper questionMapper, CommentMapper commentMapper,
                             ContentReviewLogMapper reviewLogMapper, CommentService commentService,
                             TagClassService tagClassService, NotificationService notificationService,
                             QuestionContentService questionContentService,
                             UserBatchLoader userBatchLoader, SiteCacheService siteCache) {
        this.questionMapper = questionMapper;
        this.commentMapper = commentMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.commentService = commentService;
        this.tagClassService = tagClassService;
        this.notificationService = notificationService;
        this.questionContentService = questionContentService;
        this.userBatchLoader = userBatchLoader;
        this.siteCache = siteCache;
    }

    @Override
    public PaginationDto<ReviewItemDto> listForAdmin(String type, String status, String keyword,
                                                     String page, String size) {
        ReviewTargetTypeEnum targetType = requireType(type);
        int reviewStatus = parseStatus(status);
        String search = StringUtil.sanitizeSearchKeyword(keyword);
        return targetType == ReviewTargetTypeEnum.QUESTION
                ? listQuestions(null, reviewStatus, search, page, size, false)
                : listComments(null, reviewStatus, search, page, size, false);
    }

    @Override
    public PaginationDto<ReviewItemDto> listMine(long userId, String type, String status,
                                                 String page, String size) {
        ReviewTargetTypeEnum targetType = requireType(type);
        int reviewStatus = parseStatus(status);
        return targetType == ReviewTargetTypeEnum.QUESTION
                ? listQuestions(userId, reviewStatus, null, page, size, true)
                : listComments(userId, reviewStatus, null, page, size, true);
    }

    private PaginationDto<ReviewItemDto> listQuestions(Long userId, int reviewStatus, String keyword,
                                                       String page, String size, boolean mine) {
        long count = questionMapper.selectCountByQuery(questionCondition(userId, reviewStatus, keyword, mine));
        long[] param = NumberUtils.getPageAndSize(page, size, count);
        QueryWrapper query = questionCondition(userId, reviewStatus, keyword, mine);
        // 待审核按提交时间正序（先提交先审核），其余按最新记录倒序
        query.orderBy(reviewStatus == ReviewStatusEnum.PENDING.getStatus()
                        ? QUESTION.CREATE_TIME.asc() : QUESTION.QUESTION_ID.desc())
                .select(QuestionMapper.REVIEW_QUERY_COLUMNS)
                .limit(param[0], param[1]);
        List<Question> questions = questionMapper.selectListByQuery(query);

        Set<Long> userIds = questions.stream()
                .flatMap(q -> java.util.stream.Stream.of(q.getUserId(), q.getReviewerId()))
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userBatchLoader.loadUsers(userIds);

        List<ReviewItemDto> items = new ArrayList<>(questions.size());
        for (Question question : questions) {
            items.add(toItem(question, userMap));
        }
        return pagination(items, count, param);
    }

    private QueryWrapper questionCondition(Long userId, int reviewStatus, String keyword, boolean mine) {
        QueryWrapper query = QueryWrapper.create().where(QUESTION.STATUS.eq(1));
        if (userId != null) {
            query.and(QUESTION.USER_ID.eq(userId));
        }
        if (reviewStatus >= 0) {
            query.and(QUESTION.REVIEW_STATUS.eq(reviewStatus));
        } else if (mine) {
            // 我的审核：只看待审核与未通过，已通过的无需展示
            query.and(QUESTION.REVIEW_STATUS.in(ReviewStatusEnum.PENDING.getStatus(),
                    ReviewStatusEnum.REJECTED.getStatus()));
        }
        if (keyword != null) {
            query.and(QUESTION.TITLE.like(keyword));
        }
        return query;
    }

    private PaginationDto<ReviewItemDto> listComments(Long userId, int reviewStatus, String keyword,
                                                      String page, String size, boolean mine) {
        long count = commentMapper.selectCountByQuery(commentCondition(userId, reviewStatus, keyword, mine));
        long[] param = NumberUtils.getPageAndSize(page, size, count);
        QueryWrapper query = commentCondition(userId, reviewStatus, keyword, mine);
        query.orderBy(reviewStatus == ReviewStatusEnum.PENDING.getStatus()
                        ? COMMENT.CREATE_TIME.asc() : COMMENT.COMMENT_ID.desc())
                .limit(param[0], param[1]);
        List<Comment> comments = commentMapper.selectListByQuery(query);

        Set<Long> questionIds = comments.stream().map(Comment::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of()
                : questionMapper.selectListByQuery(QueryWrapper.create()
                            .select(QUESTION.QUESTION_ID, QUESTION.TITLE)
                            .where(QUESTION.QUESTION_ID.in(questionIds)))
                        .stream().collect(Collectors.toMap(Question::getQuestionId, it -> it));
        Set<Long> userIds = comments.stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getCommentator(), c.getReviewerId()))
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userBatchLoader.loadUsers(userIds);

        List<ReviewItemDto> items = new ArrayList<>(comments.size());
        for (Comment comment : comments) {
            items.add(toItem(comment, questionMap.get(comment.getQuestionId()), userMap));
        }
        return pagination(items, count, param);
    }

    private QueryWrapper commentCondition(Long userId, int reviewStatus, String keyword, boolean mine) {
        QueryWrapper query = QueryWrapper.create().where(COMMENT.STATUS.eq(1));
        if (userId != null) {
            query.and(COMMENT.COMMENTATOR.eq(userId));
        }
        if (reviewStatus >= 0) {
            query.and(COMMENT.REVIEW_STATUS.eq(reviewStatus));
        } else if (mine) {
            query.and(COMMENT.REVIEW_STATUS.in(ReviewStatusEnum.PENDING.getStatus(),
                    ReviewStatusEnum.REJECTED.getStatus()));
        }
        if (keyword != null) {
            query.and(COMMENT.CONTENT.like(keyword));
        }
        return query;
    }

    @Override
    public ReviewDetailDto detail(String type, long id) {
        ReviewTargetTypeEnum targetType = requireType(type);
        ReviewDetailDto dto = new ReviewDetailDto();
        if (targetType == ReviewTargetTypeEnum.QUESTION) {
            Question question = questionMapper.selectOneById(id);
            if (question == null) {
                throw new CustomizeException(CustomizeErrorCode.REVIEW_NOT_FOUND);
            }
            dto.setItem(toItem(question, loadUsers(question.getUserId(), question.getReviewerId())));
            // 管理员审核必须看到全部正文，包括密码可见/回复可见段落
            dto.setContents(questionContentService.visibleContents(
                    question.getQuestionId(), question.getUserId(), null, true));
            return dto;
        }

        Comment comment = commentMapper.selectOneById(id);
        if (comment == null) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_NOT_FOUND);
        }
        Question question = questionMapper.selectOneById(comment.getQuestionId());
        dto.setItem(toItem(comment, question, loadUsers(comment.getCommentator(), comment.getReviewerId())));
        dto.setContents(List.of());
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String type, long id, User admin) {
        ReviewTargetTypeEnum targetType = requireType(type);
        long now = System.currentTimeMillis();
        if (targetType == ReviewTargetTypeEnum.QUESTION) {
            Question question = questionMapper.selectOneById(id);
            if (question == null || question.getStatus() == 0) {
                throw new CustomizeException(CustomizeErrorCode.REVIEW_NOT_FOUND);
            }
            if (questionMapper.approveReview(id, admin.getId(), now) != 1) {
                throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
            }
            // 审核通过后问题才公开，话题讨论数在此刻 +1
            if (question.getTopicId() != null && question.getTopicId() > 0) {
                tagClassService.updateTalkCount(question.getTopicId(), 1);
            }
            writeLog(ReviewTargetTypeEnum.QUESTION, id, ReviewActionEnum.APPROVE, admin.getId(), null, null);
            notificationService.createSystemNotification(question.getUserId(), admin.getId(),
                    question.getQuestionId(), -1, NotificationTypeEnum.REVIEW_QUESTION_PASS.getType(),
                    "你的问题已通过审核，现已公开展示：「" + StringUtil.subString(question.getTitle(), 40) + "」");
            invalidateQuestionCache();
            log.info("管理员 {} 审核通过问题 {}", admin.getId(), id);
            return;
        }

        Comment comment = commentMapper.selectOneById(id);
        if (comment == null || comment.getStatus() == 0) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_NOT_FOUND);
        }
        if (commentMapper.approveReview(id, admin.getId(), now) != 1) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
        }
        // 通过后才计数并通知被回复人，避免待审评论影响问题回复数与消息中心
        commentService.publishApprovedComment(comment);
        writeLog(ReviewTargetTypeEnum.COMMENT, id, ReviewActionEnum.APPROVE, admin.getId(), null, null);
        notificationService.createSystemNotification(comment.getCommentator(), admin.getId(),
                comment.getQuestionId(), comment.getCommentId(),
                NotificationTypeEnum.REVIEW_COMMENT_PASS.getType(),
                "你的评论已通过审核，现已公开展示：" + StringUtil.subString(comment.getContent(), 100));
        CacheInvalidation.afterCommit(() -> siteCache.invalidate(SiteCacheService.HOME));
        log.info("管理员 {} 审核通过评论 {}", admin.getId(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String type, long id, String reason, String suggestion, User admin) {
        ReviewTargetTypeEnum targetType = requireType(type);
        String cleanReason = StringUtils.trimToNull(reason);
        if (cleanReason == null) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_REASON_REQUIRED);
        }
        if (cleanReason.length() > MAX_REASON_LENGTH
                || (suggestion != null && suggestion.length() > MAX_SUGGESTION_LENGTH)) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                    "审核原因与修改意见均不能超过 " + MAX_REASON_LENGTH + " 个字符");
        }
        String cleanSuggestion = StringUtils.trimToNull(suggestion);
        long now = System.currentTimeMillis();

        if (targetType == ReviewTargetTypeEnum.QUESTION) {
            Question question = questionMapper.selectOneById(id);
            if (question == null || question.getStatus() == 0) {
                throw new CustomizeException(CustomizeErrorCode.REVIEW_NOT_FOUND);
            }
            if (questionMapper.rejectReview(id, cleanReason, cleanSuggestion, admin.getId(), now) != 1) {
                throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
            }
            writeLog(ReviewTargetTypeEnum.QUESTION, id, ReviewActionEnum.REJECT, admin.getId(),
                    cleanReason, cleanSuggestion);
            notificationService.createSystemNotification(question.getUserId(), admin.getId(),
                    question.getQuestionId(), -1, NotificationTypeEnum.REVIEW_QUESTION_REJECT.getType(),
                    rejectContent("问题", question.getTitle(), cleanReason, cleanSuggestion));
            log.info("管理员 {} 驳回问题 {}：{}", admin.getId(), id, cleanReason);
            return;
        }

        Comment comment = commentMapper.selectOneById(id);
        if (comment == null || comment.getStatus() == 0) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_NOT_FOUND);
        }
        if (commentMapper.rejectReview(id, cleanReason, cleanSuggestion, admin.getId(), now) != 1) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
        }
        writeLog(ReviewTargetTypeEnum.COMMENT, id, ReviewActionEnum.REJECT, admin.getId(),
                cleanReason, cleanSuggestion);
        notificationService.createSystemNotification(comment.getCommentator(), admin.getId(),
                comment.getQuestionId(), comment.getCommentId(),
                NotificationTypeEnum.REVIEW_COMMENT_REJECT.getType(),
                rejectContent("评论", comment.getContent(), cleanReason, cleanSuggestion));
        log.info("管理员 {} 驳回评论 {}：{}", admin.getId(), id, cleanReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitComment(long commentId, String content, User user) {
        String cleanContent = content == null ? "" : content.trim();
        if (cleanContent.isEmpty()) {
            throw new CustomizeException(CustomizeErrorCode.CONTENT_IS_EMPTY);
        }
        if (cleanContent.length() > MAX_COMMENT_LENGTH) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                    "评论内容不能超过 " + MAX_COMMENT_LENGTH + " 个字符");
        }
        Comment comment = commentMapper.selectOneById(commentId);
        if (comment == null || comment.getStatus() == 0) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_NOT_FOUND);
        }
        if (comment.getCommentator() != user.getId()) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        if (comment.getReviewStatus() != ReviewStatusEnum.REJECTED.getStatus()) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_RESUBMIT_FORBIDDEN);
        }
        if (commentMapper.resubmitReview(commentId, user.getId(), cleanContent,
                System.currentTimeMillis()) != 1) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_RESUBMIT_FORBIDDEN);
        }
        writeLog(ReviewTargetTypeEnum.COMMENT, commentId, ReviewActionEnum.SUBMIT, user.getId(), null, null);
        notificationService.createSystemNotification(user.getId(), user.getId(),
                comment.getQuestionId(), commentId,
                NotificationTypeEnum.REVIEW_COMMENT_PENDING.getType(),
                "你的评论已修改并重新提交审核，通过后将公开展示：" + StringUtil.subString(cleanContent, 100));
        log.info("用户 {} 重新提交评论 {} 进入审核", user.getId(), commentId);
    }

    @Override
    public Map<String, Long> pendingCounts() {
        long questionCount = questionMapper.selectCountByQuery(QueryWrapper.create()
                .where(QUESTION.STATUS.eq(1))
                .and(QUESTION.REVIEW_STATUS.eq(ReviewStatusEnum.PENDING.getStatus())));
        long commentCount = commentMapper.selectCountByQuery(QueryWrapper.create()
                .where(COMMENT.STATUS.eq(1))
                .and(COMMENT.REVIEW_STATUS.eq(ReviewStatusEnum.PENDING.getStatus())));
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("question", questionCount);
        counts.put("comment", commentCount);
        counts.put("total", questionCount + commentCount);
        return counts;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private ReviewTargetTypeEnum requireType(String type) {
        ReviewTargetTypeEnum targetType = ReviewTargetTypeEnum.of(type);
        if (targetType == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        return targetType;
    }

    /** 默认只看待审核；-1 表示全部；其余必须是合法的审核状态 */
    private int parseStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return ReviewStatusEnum.PENDING.getStatus();
        }
        int value;
        try {
            value = Integer.parseInt(status.trim());
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        if (value == -1 || ReviewStatusEnum.isExist(value)) {
            return value;
        }
        throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
    }

    private ReviewItemDto toItem(Question question, Map<Long, User> userMap) {
        ReviewItemDto item = baseItem(question.getQuestionId(), ReviewTargetTypeEnum.QUESTION,
                question.getUserId(), question.getReviewerId(), userMap);
        item.setQuestionId(question.getQuestionId());
        item.setQuestionTitle(question.getTitle());
        item.setTitle(question.getTitle());
        item.setClassification(question.getClassification());
        item.setTag(question.getTag());
        item.setReviewStatus(question.getReviewStatus());
        item.setStatus(question.getStatus());
        item.setCreateTime(StringUtil.foematTime(question.getCreateTime()));
        if (question.getReviewTime() > 0) {
            item.setReviewTime(StringUtil.foematTime(question.getReviewTime()));
        }
        item.setReviewReason(question.getReviewReason());
        item.setReviewSuggestion(question.getReviewSuggestion());
        return item;
    }

    private ReviewItemDto toItem(Comment comment, Question question, Map<Long, User> userMap) {
        ReviewItemDto item = baseItem(comment.getCommentId(), ReviewTargetTypeEnum.COMMENT,
                comment.getCommentator(), comment.getReviewerId(), userMap);
        item.setQuestionId(comment.getQuestionId());
        item.setQuestionTitle(question == null ? null : question.getTitle());
        item.setTitle(StringUtil.subString(comment.getContent(), 40));
        item.setContent(comment.getContent());
        item.setReviewStatus(comment.getReviewStatus());
        item.setStatus(comment.getStatus());
        item.setCreateTime(StringUtil.foematTime(comment.getCreateTime()));
        if (comment.getReviewTime() > 0) {
            item.setReviewTime(StringUtil.foematTime(comment.getReviewTime()));
        }
        item.setReviewReason(comment.getReviewReason());
        item.setReviewSuggestion(comment.getReviewSuggestion());
        return item;
    }

    private ReviewItemDto baseItem(long id, ReviewTargetTypeEnum type, long authorId, long reviewerId,
                                   Map<Long, User> userMap) {
        ReviewItemDto item = new ReviewItemDto();
        item.setId(id);
        item.setType(type.getType());
        item.setAuthorId(authorId);
        User author = userMap.get(authorId);
        if (author != null) {
            item.setAuthorUserId(author.getUserId());
            item.setAuthorName(author.getUserName());
            item.setAuthorHeadUrl(author.getHeadUrl());
        }
        User reviewer = userMap.get(reviewerId);
        if (reviewer != null) {
            item.setReviewerName(reviewer.getUserName());
        }
        return item;
    }

    private Map<Long, User> loadUsers(long... ids) {
        Set<Long> set = java.util.Arrays.stream(ids).filter(id -> id > 0).boxed()
                .collect(Collectors.toSet());
        return userBatchLoader.loadUsers(set);
    }

    private PaginationDto<ReviewItemDto> pagination(List<ReviewItemDto> items, long count, long[] param) {
        PaginationDto<ReviewItemDto> page = new PaginationDto<>();
        page.setData(items);
        page.setAllCount(count);
        page.setPagination(param[2], param[3], param[1]);
        return page;
    }

    private String rejectContent(String targetName, String title, String reason, String suggestion) {
        StringBuilder builder = new StringBuilder("你的")
                .append(targetName)
                .append("未通过审核。原因：")
                .append(reason);
        if (title != null && !title.isBlank()) {
            builder.append("（内容：").append(StringUtil.subString(title, 40)).append("）");
        }
        if (suggestion != null && !suggestion.isBlank()) {
            builder.append("。修改建议：").append(suggestion);
        }
        builder.append("。点击本条消息可修改并重新提交审核。");
        return builder.toString();
    }

    private void writeLog(ReviewTargetTypeEnum type, long targetId, ReviewActionEnum action,
                          long operatorId, String reason, String suggestion) {
        ContentReviewLog log = new ContentReviewLog();
        log.setTargetType(type.getType());
        log.setTargetId(targetId);
        log.setAction(action.getAction());
        log.setOperatorId(operatorId);
        log.setReason(reason);
        log.setSuggestion(suggestion);
        log.setCreateTime(System.currentTimeMillis());
        reviewLogMapper.insertSelective(log);
    }

    private void invalidateQuestionCache() {
        CacheInvalidation.afterCommit(() -> {
            siteCache.invalidate(SiteCacheService.HOME);
            siteCache.invalidate(SiteCacheService.META + "hots");
        });
    }
}
