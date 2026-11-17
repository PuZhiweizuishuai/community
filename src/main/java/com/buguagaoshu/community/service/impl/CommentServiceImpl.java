package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.CommentSortTypeEnum;
import com.buguagaoshu.community.enums.CommentTypeEnum;
import com.buguagaoshu.community.enums.NotificationStatusEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.*;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.util.DateFormatUtil;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
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

    private final UserBatchLoader userBatchLoader;


    @Autowired
    public CommentServiceImpl(CommentMapper commentMapper, QuestionMapper questionMapper,
                              NotificationMapper notificationMapper, UserBatchLoader userBatchLoader) {
        this.commentMapper = commentMapper;
        this.questionMapper = questionMapper;
        this.notificationMapper = notificationMapper;
        this.userBatchLoader = userBatchLoader;
    }


    /**
     * @Transactional spring 事务注解
     * 插入评论
     */
    @Override
    @Transactional
    public int insertComment(Comment comment) {
        if (comment.getParentId() == 0) {
            throw new CustomizeException(CustomizeErrorCode.TARGET_PARAM_NOT_FOUND);
        }
        if (!CommentTypeEnum.isExist(comment.getType())) {
            throw new CustomizeException(CustomizeErrorCode.TYPE_PARAM_WRONG);
        }

        Question question = questionMapper.selectQuestionById(comment.getQuestionId(), 1);
        if (CommentTypeEnum.COMMENT.getType().equals(comment.getType())) {
            // 回复评论
            // TODO 可能有bug 待测试
            if (question == null) {
                throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
            }
            Comment dbComment = selectCommentByCommentId(comment.getParentId());
            if (dbComment == null) {
                throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
            }
            if (comment.getParentId() != comment.getParentCommentId()) {
                Comment father = commentMapper.selectCommentByCommentId(comment.getParentCommentId(), 1);
                if (father == null) {
                    throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
                }
                father.setCommentCount(1);
                commentMapper.updateCommentCount(father);
            }

            // 创建通知
            // TODO 考虑传入评论ID
            createNotification(comment, dbComment.getCommentator(), "", StringUtil.subString(comment.getContent(), 200),
                    NotificationTypeEnum.REPLY_COMMENT, question.getQuestionId(), comment.getParentId());

            // 修改帖子修改时间
            questionMapper.alterQuestionAlterTime(System.currentTimeMillis(), comment.getQuestionId());
            // 评论评论数加 1
            dbComment.setCommentCount(1);
            commentMapper.updateCommentCount(dbComment);
            // 问题评论数加 1
            question.setCommentCount(1);
            questionMapper.updateQuestionCommentCount(question);
            return commentMapper.insertComment(comment);
        } else {
            // 回复问题
            if (question == null || (comment.getQuestionId() != comment.getParentId())) {
                throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
            }

            createNotification(comment, question.getUserId(), "", StringUtil.subString(comment.getContent(), 200),
                    NotificationTypeEnum.REPLY_QUESTION, question.getQuestionId(), -1L);
            // 修改帖子修改时间
            questionMapper.alterQuestionAlterTime(System.currentTimeMillis(), comment.getQuestionId());
            question.setCommentCount(1);
            questionMapper.updateQuestionCommentCount(question);
            return commentMapper.insertComment(comment);
        }
    }

    @Override
    public Comment selectCommentByCommentId(long commentId) {
        return commentMapper.selectCommentByCommentId(commentId, 1);
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
                .where(COMMENT.QUESTION_ID.eq(qId)).and(COMMENT.TYPE.eq(1)).and(COMMENT.STATUS.eq(1));
        long allCommentNumber = commentMapper.selectCountByQuery(query);
        // 计算分页参数
        long[] param = NumberUtils.getPageAndSize(page, size, allCommentNumber);
        QueryColumn sortColumn = switch (commentSortTypeEnum) {
            case NEW -> COMMENT.CREATE_TIME;
            case BEST_LIKE -> COMMENT.LIKE_COUNT;
            case BEST_COMMENT -> COMMENT.COMMENT_COUNT;
            default -> null;
        };
        if (sortColumn != null) {
            query.orderBy(sortColumn.desc());
        }
        List<Comment> comments = commentMapper.selectListByQuery(query.limit(param[0], param[1]));


        if (comments.size() == 0) {
            return null;
        }
        // 获取该问题下去重的所有评论者，批量装配作者信息
        Set<Long> commentors = comments.stream().map(Comment::getCommentator).collect(Collectors.toSet());
        Map<Long, User> userMap = userBatchLoader.loadUsers(commentors, true);

        // 为评论添加作者信息
        List<CommentDto> commentDtos = comments.stream().map(comment -> {
            CommentDto commentDto = new CommentDto();
            BeanUtils.copyProperties(comment, commentDto);

            commentDto.setCreateTime(DateFormatUtil.getTimeDifference(comment.getCreateTime(), System.currentTimeMillis()));
            commentDto.setModifiedTime(DateFormatUtil.getTimeDifference(comment.getModifiedTime(), System.currentTimeMillis()));
            commentDto.setUser(userMap.get(comment.getCommentator()));
            return commentDto;
        }).collect(Collectors.toList());

        PaginationDto<CommentDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(commentDtos);
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;
    }

    @Override
    public List<CommentDto> getTwoLevelCommentByParent(String parentId, int type) {
        Long id;
        try {
            id = Long.valueOf(parentId);
        } catch (Exception e) {
            throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
        }
        List<Comment> comments = commentMapper.selectListByQuery(QueryWrapper.create()
                .where(COMMENT.PARENT_COMMENT_ID.eq(id)).and(COMMENT.TYPE.eq(type)).and(COMMENT.STATUS.eq(1)));
        if (comments.size() == 0) {
            throw new CustomizeException(CustomizeErrorCode.COMMENT_NO_COMMENT);
        }
        Set<Long> commentors = comments.stream().map(Comment::getCommentator).collect(Collectors.toSet());

        // 二级回复可能指向别的评论：批量取目标评论与目标用户，避免逐条查询
        Set<Long> targetCommentIds = comments.stream()
                .filter(c -> c.getParentCommentId() != c.getParentId())
                .map(Comment::getParentId)
                .collect(Collectors.toSet());
        Map<Long, Comment> targetCommentMap = targetCommentIds.isEmpty() ? Map.of()
                : commentMapper.selectListByIds(targetCommentIds).stream()
                        .collect(Collectors.toMap(Comment::getCommentId, Function.identity()));
        Set<Long> allUserIds = new HashSet<>(commentors);
        targetCommentMap.values().forEach(c -> allUserIds.add(c.getCommentator()));
        Map<Long, User> userMap = userBatchLoader.loadUsers(allUserIds, true);

        // 为评论添加作者信息
        List<CommentDto> commentDtos = comments.stream().map(comment -> {
            CommentDto commentDto = new CommentDto();
            if (comment.getParentCommentId() != comment.getParentId()) {
                Comment targetComment = targetCommentMap.get(comment.getParentId());
                User targetUser = targetComment == null ? null : userMap.get(targetComment.getCommentator());
                if (targetUser != null) {
                    commentDto.setTargetName(targetUser.getUserName());
                    commentDto.setTargetUrl("/user/" + targetUser.getUserId());
                }
            }
            BeanUtils.copyProperties(comment, commentDto);
            commentDto.setUser(userMap.get(comment.getCommentator()));
            commentDto.setCreateTime(DateFormatUtil.getTimeDifference(comment.getCreateTime(), System.currentTimeMillis()));
            commentDto.setModifiedTime(DateFormatUtil.getTimeDifference(comment.getModifiedTime(), System.currentTimeMillis()));
            return commentDto;
        }).collect(Collectors.toList());

        return commentDtos;
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
