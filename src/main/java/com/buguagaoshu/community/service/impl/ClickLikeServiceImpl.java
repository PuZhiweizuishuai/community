package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.ClickLikeDTO;
import com.buguagaoshu.community.enums.ClickLikeTypeEnum;
import com.buguagaoshu.community.enums.NotificationStatusEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.*;
import com.buguagaoshu.community.model.ClickLike;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Notification;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.ClickLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 16:11
 */
@Service
public class ClickLikeServiceImpl implements ClickLikeService {
    private final ClickLikeMapper clickLikeMapper;

    private final QuestionMapper questionMapper;

    private final CommentMapper commentMapper;

    private final NotificationMapper notificationMapper;

    private final UserMapper userMapper;

    @Autowired
    public ClickLikeServiceImpl(ClickLikeMapper clickLikeMapper, QuestionMapper questionMapper,
                                CommentMapper commentMapper, NotificationMapper notificationMapper,
                                UserMapper userMapper) {
        this.clickLikeMapper = clickLikeMapper;
        this.questionMapper = questionMapper;
        this.commentMapper = commentMapper;
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = CustomizeException.class)
    public ClickLikeTypeEnum createClickLike(ClickLikeDTO clickLikeDTO) {
        ClickLike clickLike = new ClickLike();
        clickLike.setNotifier(clickLikeDTO.getNotifier());
        clickLike.setNotifierName(clickLikeDTO.getNotifierName());
        clickLike.setReceiver(clickLikeDTO.getReceiver());
        // 防止问题不存在
        Question question = questionMapper.selectQuestionById(clickLikeDTO.getQuestionId(), 1);
        if(question == null) {
            return ClickLikeTypeEnum.LIKE_QUESTION_NOT_FOUND;
        }
        clickLike.setQuestionId(clickLikeDTO.getQuestionId());
        clickLike.setCommentId(clickLikeDTO.getCommentId());

        // 判断类型
        if(clickLikeDTO.getCommentId() == -1) {
            if(clickLikeMapper.getClickLikePreventRepeat(clickLike.getNotifier(), clickLike.getQuestionId(), -1) != null) {
                return ClickLikeTypeEnum.REPEAT_LIKE;
            }
            clickLike.setType(ClickLikeTypeEnum.LIKE_QUESTION.getType());
            clickLike.setCreateTime(System.currentTimeMillis());
            // 插入点赞表
            clickLikeMapper.createClickLike(clickLike);
            // 更新问题点赞数
            question.setLikeCount(1);
            questionMapper.updateQuestionLikeCount(question);
            // 创建通知
            createNotification(clickLike, NotificationTypeEnum.LIKE_QUESTION, clickLike.getQuestionId());
            // 用户获赞数加一
            userMapper.updateUserLikeCount(1, clickLike.getReceiver());
            return ClickLikeTypeEnum.SUCCESS;

        } else {
            Comment comment = commentMapper.selectCommentByCommentId(clickLikeDTO.getCommentId(), 1);
            if(comment == null) {
                return ClickLikeTypeEnum.LIKE_COMMENT_NOT_FOUND;
            }
            if(clickLikeMapper.getClickLikePreventRepeat(clickLike.getNotifier(), clickLike.getQuestionId(), comment.getCommentId()) != null) {
                return ClickLikeTypeEnum.REPEAT_LIKE;
            }
            clickLike.setType(ClickLikeTypeEnum.LIKE_COMMENT.getType());
            clickLike.setCreateTime(System.currentTimeMillis());
            clickLikeMapper.createClickLike(clickLike);
            // 更新评论点赞数
            comment.setLikeCount(1);
            commentMapper.updateCommentCount(comment);
            // 创建通知
            createNotification(clickLike, NotificationTypeEnum.LIKE_COMMENT, clickLike.getCommentId());
            // 用户获赞加 1
            userMapper.updateUserLikeCount(1, clickLike.getReceiver());
            return ClickLikeTypeEnum.SUCCESS;
        }
    }

    @Override
    public Boolean isClickLikeQuestion(ClickLike clickLike) {
        if(clickLikeMapper.getClickLikePreventRepeat(clickLike.getNotifier(), clickLike.getQuestionId(), clickLike.getCommentId()) != null) {
            return true;
        }
        return false;
    }


    private void createNotification(ClickLike clickLike, NotificationTypeEnum notificationType,
                                    Long outerId) {
        if(clickLike.getReceiver() == clickLike.getNotifier()) {
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
        notification.setNotifier(clickLike.getNotifier());
        // 通知状态
        notification.setStatus(NotificationStatusEnum.UNREAD.getStatus());
        // 通知接收人
        notification.setReceiver(clickLike.getReceiver());
        notificationMapper.insertNotification(notification);
    }
}
