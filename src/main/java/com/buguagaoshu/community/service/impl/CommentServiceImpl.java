package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.CommentSortTypeEnum;
import com.buguagaoshu.community.enums.CommentTypeEnum;
import com.buguagaoshu.community.enums.NotificationStatusEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.*;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.SystemConfigService;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.buguagaoshu.community.model.table.CommentTableDef.COMMENT;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:19
 */
@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;

    private final QuestionMapper questionMapper;

    private final NotificationMapper notificationMapper;

    private final NotificationService notificationService;

    private final UserBatchLoader userBatchLoader;

    private final SystemConfigService systemConfigService;


    @Autowired
    public CommentServiceImpl(CommentMapper commentMapper, QuestionMapper questionMapper,
                              NotificationMapper notificationMapper, NotificationService notificationService,
                              UserBatchLoader userBatchLoader, SystemConfigService systemConfigService) {
        this.commentMapper = commentMapper;
        this.questionMapper = questionMapper;
        this.notificationMapper = notificationMapper;
        this.notificationService = notificationService;
        this.userBatchLoader = userBatchLoader;
        this.systemConfigService = systemConfigService;
    }


    /**
     * @Transactional spring 事务注解
     * 插入评论：审核开启时先进入待审核区，通过后才计数并通知被回复人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentDto insertComment(Comment comment) {
        if (comment.getParentId() <= 0) {
            throw new CustomizeException(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND);
        }
        if (!CommentTypeEnum.isExist(comment.getType())) {
            throw new CustomizeException(CustomizeErrorCode.TYPE_PARAM_WRONG);
        }

        Question question = questionMapper.selectQuestionById(comment.getQuestionId(), 1);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        // 未公开的问题不允许继续产生评论，避免绕过审核在待审帖子下互动
        if (question.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()) {
            throw new CustomizeException(CustomizeErrorCode.CONTENT_UNDER_REVIEW);
        }

        boolean reviewRequired = systemConfigService.isReviewEnabled();
        comment.setReviewStatus(reviewRequired
                ? ReviewStatusEnum.PENDING.getStatus() : ReviewStatusEnum.APPROVED.getStatus());

        // 回复链路只加载一次目标与根评论，后续计数/通知直接复用内存对象
        Comment target = null;
        Comment root = null;
        if (CommentTypeEnum.COMMENT.getType().equals(comment.getType())) {
            target = selectCommentByCommentId(comment.getParentId());
            if (target == null || target.getQuestionId() != question.getQuestionId()) {
                throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
            }
            root = resolveRootComment(target);
            if (root == null || root.getType() != CommentTypeEnum.QUESTION.getType()) {
                throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
            }
            comment.setParentCommentId(root.getCommentId());
            comment.setParentId(target.getCommentId());
        } else {
            if (comment.getParentId() != question.getQuestionId()) {
                throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
            }
            comment.setParentCommentId(-1);
        }
        commentMapper.insertComment(comment);
        // 数据库默认 status=1，内存对象同步该值，避免直接组装 DTO 时状态为 0
        comment.setStatus(1);

        if (reviewRequired) {
            notifyReviewSubmitted(comment);
        } else {
            publishApprovedComment(comment, question, target, root);
        }

        return toCommentDtos(List.of(comment)).get(0);
    }

    /**
     * 审核通过后的发布副作用：问题最后活跃时间、问题/根评论回复数、回复通知。
     * 该方法只应在评论真正公开时调用一次（审核关闭时插入后立即调用，审核开启时审核通过后调用）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishApprovedComment(Comment comment) {
        Question question = questionMapper.selectQuestionById(comment.getQuestionId(), 1);
        if (question == null) {
            return;
        }
        Comment target = null;
        Comment root = null;
        if (comment.getType() == CommentTypeEnum.COMMENT.getType()) {
            root = selectCommentByCommentId(comment.getParentCommentId());
            target = selectCommentByCommentId(comment.getParentId());
        }
        publishApprovedComment(comment, question, target, root);
    }

    /** 发布副作用内部实现：question/target/root 由调用方预先加载，避免重复 SELECT */
    private void publishApprovedComment(Comment comment, Question question, Comment target, Comment root) {
        // 新评论会改变问题的最后活跃时间与回复总数
        questionMapper.alterQuestionAlterTime(System.currentTimeMillis(), comment.getQuestionId());
        Question countUpdate = new Question();
        countUpdate.setQuestionId(question.getQuestionId());
        countUpdate.setCommentCount(1);
        questionMapper.updateQuestionCommentCount(countUpdate);

        if (comment.getType() == CommentTypeEnum.COMMENT.getType()) {
            // 只累加根评论的回复数；二级评论不再承载回复计数
            if (root != null) {
                Comment rootCount = new Comment();
                rootCount.setCommentId(root.getCommentId());
                rootCount.setCommentCount(1);
                commentMapper.updateCommentCount(rootCount);
            }
            if (target != null) {
                createNotification(comment, target.getCommentator(), "",
                        StringUtil.subString(comment.getContent(), 200),
                        NotificationTypeEnum.REPLY_COMMENT, question.getQuestionId(), target.getCommentId());
            }
        } else {
            createNotification(comment, question.getUserId(), "",
                    StringUtil.subString(comment.getContent(), 200),
                    NotificationTypeEnum.REPLY_QUESTION, question.getQuestionId(), -1L);
        }
    }

    /**
     * 评论进入审核区时给作者一条系统消息，解释为什么暂时看不到自己的评论。
     */
    private void notifyReviewSubmitted(Comment comment) {
        String summary = "你的评论已提交审核，通过后将公开展示：" + StringUtil.subString(comment.getContent(), 100);
        notificationService.createSystemNotification(comment.getCommentator(), comment.getCommentator(),
                comment.getQuestionId(), comment.getCommentId(),
                NotificationTypeEnum.REVIEW_COMMENT_PENDING.getType(), summary);
    }

    /**
     * 向上查找根评论，兼容历史上「回复二级评论」把 parentCommentId 写成二级评论 ID 的数据。
     */
    private Comment resolveRootComment(Comment comment) {
        Comment root = comment;
        for (int depth = 0; depth < 10 && root != null
                && root.getType() == CommentTypeEnum.COMMENT.getType(); depth++) {
            root = selectCommentByCommentId(root.getParentCommentId());
        }
        return root;
    }

    @Override
    public Comment selectCommentByCommentId(long commentId) {
        return commentMapper.selectCommentByCommentId(commentId, 1);
    }

    /**
     * 作者删除自己的评论：软删除 + 计数回退。
     *
     * <p>与管理员删除的区别：不发送「已被管理员删除」通知，仅作者本人可操作。
     * 被删除评论下的二级回复保留（与后台删除行为一致），前端按评论状态过滤展示。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnComment(long commentId, User user) {
        Comment comment = commentMapper.selectOneById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "评论不存在或已被删除");
        }
        if (comment.getCommentator() != user.getId()) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        // 条件更新：并发下只有一个请求能把 1 改成 0
        if (commentMapper.alterStatus(commentId, 0, 1) != 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT);
        }
        if (comment.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
            adjustCommentCount(comment, -1);
        }
    }

    /** 删除评论后回退计数：问题回复数始终回退，二级回复同时回退根评论的回复数 */
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

    @Override
    public PaginationDto<CommentDto> getCommentDtoByQuestionIdForQuestion(String questionId, String page, String size, CommentSortTypeEnum commentSortTypeEnum) {
        long qId;
        try {
            qId = Long.valueOf(questionId);
        } catch (Exception e) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        QueryWrapper query = QueryWrapper.create()
                .where(COMMENT.QUESTION_ID.eq(qId))
                .and(COMMENT.TYPE.eq(CommentTypeEnum.QUESTION.getType()))
                .and(COMMENT.STATUS.eq(1))
                .and(COMMENT.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()));
        long allCommentNumber = commentMapper.selectCountByQuery(query);
        // 计算分页参数
        long[] param = NumberUtils.getPageAndSize(page, size, allCommentNumber);
        // 排序值相同的评论用时间与 ID 兜底，保证翻页结果稳定
        switch (commentSortTypeEnum) {
            case NEW -> query.orderBy(COMMENT.CREATE_TIME.desc(), COMMENT.COMMENT_ID.desc());
            case BEST_LIKE -> query.orderBy(COMMENT.LIKE_COUNT.desc(), COMMENT.CREATE_TIME.asc(), COMMENT.COMMENT_ID.asc());
            case BEST_COMMENT -> query.orderBy(COMMENT.COMMENT_COUNT.desc(), COMMENT.CREATE_TIME.asc(), COMMENT.COMMENT_ID.asc());
            default -> query.orderBy(COMMENT.CREATE_TIME.asc(), COMMENT.COMMENT_ID.asc());
        }
        List<Comment> comments = commentMapper.selectListByQuery(query.limit(param[0], param[1]));

        PaginationDto<CommentDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(toCommentDtos(comments));
        paginationDto.setAllCount(allCommentNumber);
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;
    }

    @Override
    public PaginationDto<CommentDto> getTwoLevelCommentByParent(String parentId, String page, String size) {
        long id;
        try {
            id = Long.valueOf(parentId);
        } catch (Exception e) {
            throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
        }
        QueryWrapper query = QueryWrapper.create()
                .where(COMMENT.PARENT_COMMENT_ID.eq(id))
                .and(COMMENT.TYPE.eq(CommentTypeEnum.COMMENT.getType()))
                .and(COMMENT.STATUS.eq(1))
                .and(COMMENT.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()));
        long allCount = commentMapper.selectCountByQuery(query);
        long[] param = NumberUtils.getPageAndSize(page, size, allCount);
        List<Comment> comments = commentMapper.selectListByQuery(query
                .orderBy(COMMENT.CREATE_TIME.asc(), COMMENT.COMMENT_ID.asc())
                .limit(param[0], param[1]));

        PaginationDto<CommentDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(toCommentDtos(comments));
        paginationDto.setAllCount(allCount);
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;
    }

    /**
     * 批量装配评论展示数据：作者信息与「回复 @xxx」的目标用户，避免逐条查询。
     */
    private List<CommentDto> toCommentDtos(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = comments.stream().map(Comment::getCommentator).collect(Collectors.toSet());

        // 只有回复了二级评论才需要展示 @ 对象：reply 的 parentId 与 parentCommentId 不相等
        Set<Long> targetCommentIds = comments.stream()
                .filter(c -> c.getType() == CommentTypeEnum.COMMENT.getType())
                .filter(c -> c.getParentId() != c.getParentCommentId())
                .map(Comment::getParentId)
                .collect(Collectors.toSet());
        Map<Long, Comment> targetCommentMap = targetCommentIds.isEmpty() ? Map.of()
                : commentMapper.selectListByIds(targetCommentIds).stream()
                        .collect(Collectors.toMap(Comment::getCommentId, Function.identity()));
        targetCommentMap.values().forEach(c -> userIds.add(c.getCommentator()));

        Map<Long, User> userMap = userBatchLoader.loadUsers(userIds, true);
        return comments.stream().map(comment -> {
            CommentDto commentDto = new CommentDto();
            BeanUtils.copyProperties(comment, commentDto);
            commentDto.setUser(userMap.get(comment.getCommentator()));

            if (comment.getType() == CommentTypeEnum.COMMENT.getType()
                    && comment.getParentId() != comment.getParentCommentId()) {
                Comment targetComment = targetCommentMap.get(comment.getParentId());
                User targetUser = targetComment == null ? null : userMap.get(targetComment.getCommentator());
                if (targetUser != null) {
                    commentDto.setTargetName(targetUser.getUserName());
                    commentDto.setTargetUrl("/user/" + targetUser.getUserId());
                }
            }
            return commentDto;
        }).collect(Collectors.toList());
    }

    /**
     * 创建通知
     */
    private void createNotification(Comment comment, Long receiver, String notifierName,
                                    String commentContent, NotificationTypeEnum notificationType,
                                    Long outerId, Long commentId) {
        if (receiver == comment.getCommentator()) {
            return;
        }
        Notification notification = new Notification();

        // 消息创建时间
        notification.setCreateTime(System.currentTimeMillis());
        // 消息类型
        notification.setType(notificationType.getType());
        // 通知产生点
        notification.setOuterId(outerId);
        // 通知发起人
        notification.setNotifier(comment.getCommentator());
        // 通知状态
        notification.setStatus(NotificationStatusEnum.UNREAD.getStatus());
        // 通知接收人
        notification.setReceiver(receiver);

        // 缓存评论内容
        notification.setCommentContent(commentContent);

        notification.setCommentId(commentId);

        notificationMapper.insertSelective(notification);
    }
}
