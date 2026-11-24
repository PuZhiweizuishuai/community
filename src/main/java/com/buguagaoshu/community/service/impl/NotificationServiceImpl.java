package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.NotificationStatusEnum;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Notification;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.util.NotificationTextUtil;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 13:17
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /** 前端据 actorType 决定前缀是头像昵称还是「系统」徽章 */
    private static final String ACTOR_USER = "user";
    private static final String ACTOR_SYSTEM = "system";
    private static final String ACTOR_SYSTEM_NAME = "系统";

    private final NotificationMapper notificationMapper;

    private final QuestionMapper questionMapper;

    private final CommentMapper commentMapper;

    private final TagClassMapper tagClassMapper;

    private final UserBatchLoader userBatchLoader;

    @Autowired
    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   QuestionMapper questionMapper, CommentMapper commentMapper,
                                   TagClassMapper tagClassMapper,
                                   UserBatchLoader userBatchLoader) {
        this.notificationMapper = notificationMapper;
        this.questionMapper = questionMapper;
        this.commentMapper = commentMapper;
        this.tagClassMapper = tagClassMapper;
        this.userBatchLoader = userBatchLoader;
    }

    @Override
    public long getAllNotificationNumber(long id) {
        if (id <= 0) {
            return 0;
        }
        return notificationMapper.getAllNotificationNumber(id);
    }

    @Override
    public long getAllNotificationNoReadNumber(long id) {
        if (id <= 0) {
            return 0;
        }
        return notificationMapper.getAllNotificationNoReadNumber(id);
    }

    @Override
    public PaginationDto<NotificationDTO> getAllNotification(String page, String size, long id, String type) {
        Integer types = null;
        try {
            types = Integer.valueOf(type);
        } catch (Exception e) {
            types = 1;
        }
        long allNotification;
        long[] param;
        List<Notification> notifications = null;
        // 获取回复通知
        if (types == 5) {
            allNotification = notificationMapper.getAllSystemCount(id);
            param = NumberUtils.getPageAndSize(page, size, allNotification);
            notifications = notificationMapper.getAllSystemNotification(param[0], param[1], id);
        } else if (types == 3) {
            allNotification = notificationMapper.getAllLikeCount(id);
            param = NumberUtils.getPageAndSize(page, size, allNotification);
            notifications = notificationMapper.getAllLikeNotification(param[0], param[1], id);
        } else {
            // 获取所有回复数
            allNotification = notificationMapper.getAllCommentCount(id);
            // 计算分页参数
            param = NumberUtils.getPageAndSize(page, size, allNotification);
            notifications = notificationMapper.getAllCommentNotification(param[0], param[1], id);
        }

        if (notifications.size() == 0) {
            return null;
        }

        Set<Long> disUserIds = notifications.stream().map(Notification::getNotifier).collect(Collectors.toSet());
        Map<Long, User> userMap = userBatchLoader.loadUsers(disUserIds);

        // 批量取通知指向的评论/问题，避免逐条查询；已删除内容展示占位而不是 NPE
        Set<Long> commentIds = notifications.stream()
                .filter(n -> n.getType() == NotificationTypeEnum.LIKE_COMMENT.getType()
                        || n.getType() == NotificationTypeEnum.REPLY_COMMENT.getType())
                .map(Notification::getCommentId)
                .filter(cid -> cid > 0)
                .collect(Collectors.toSet());
        Map<Long, Comment> commentMap = commentIds.isEmpty() ? Map.of()
                : commentMapper.selectListByIds(commentIds).stream()
                        .collect(Collectors.toMap(Comment::getCommentId, Function.identity()));
        Set<Long> questionIds = notifications.stream()
                .filter(n -> n.getType() != NotificationTypeEnum.LIKE_COMMENT.getType()
                        && n.getType() != NotificationTypeEnum.REPLY_COMMENT.getType()
                        && !NotificationTypeEnum.isTopicType(n.getType())
                        && !NotificationTypeEnum.isTagType(n.getType()))
                .map(Notification::getOuterId)
                .filter(qid -> qid > 0)
                .collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of()
                : questionMapper.selectListByIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
        Set<Long> topicIds = notifications.stream()
                .filter(n -> NotificationTypeEnum.isTopicType(n.getType()))
                .map(Notification::getOuterId)
                .filter(tid -> tid > 0)
                .collect(Collectors.toSet());
        Map<Long, TagClass> topicMap = topicIds.isEmpty() ? Map.of()
                : tagClassMapper.selectListByIds(topicIds).stream()
                        .collect(Collectors.toMap(TagClass::getId, Function.identity()));

        List<NotificationDTO> notificationDTOS = new ArrayList<>();
        for (Notification notification : notifications) {
            NotificationTypeEnum typeEnum = NotificationTypeEnum.typeOf(notification.getType());
            NotificationDTO notificationDTO = new NotificationDTO();

            notificationDTO.setId(notification.getId());
            notificationDTO.setCreateTime(StringUtil.foematTime(notification.getCreateTime()));
            notificationDTO.setTypeName(typeEnum == null ? "" : typeEnum.getName());
            notificationDTO.setType(notification.getType());
            notificationDTO.setStatus(notification.getStatus());

            // 系统通知不暴露处理人身份，通知表仍保留 notifier 供后台审计
            if (typeEnum == null || typeEnum.isSystemActor()) {
                notificationDTO.setActorType(ACTOR_SYSTEM);
                notificationDTO.setNotifierName(ACTOR_SYSTEM_NAME);
            } else {
                notificationDTO.setActorType(ACTOR_USER);
                notificationDTO.setNotifier(notification.getNotifier());
                User notifier = userMap.get(notification.getNotifier());
                notificationDTO.setNotifierId(notifier == null ? null : notifier.getUserId());
                notificationDTO.setNotifierName(notifier == null ? "已注销用户" : notifier.getUserName());
                notificationDTO.setNotifierAvatar(notifier == null ? null : notifier.getHeadUrl());
            }

            if (notification.getType() == NotificationTypeEnum.LIKE_COMMENT.getType()
                    || notification.getType() == NotificationTypeEnum.REPLY_COMMENT.getType()) {
                Comment comment = commentMap.get(notification.getCommentId());
                notificationDTO.setOuterTitle(comment == null || comment.getStatus() == 0
                        ? "内容已删除" : NotificationTextUtil.shortText(comment.getContent(), 25));
            } else if (NotificationTypeEnum.isTopicType(notification.getType())) {
                TagClass topic = topicMap.get(notification.getOuterId());
                notificationDTO.setOuterTitle(topic == null ? "话题已删除" : topic.getTitle());
            } else if (NotificationTypeEnum.isTagType(notification.getType())) {
                // 标签通知没有详情页，标题置空避免误显示「内容已删除」和错误跳转
                notificationDTO.setOuterTitle(null);
            } else {
                Question question = questionMap.get(notification.getOuterId());
                notificationDTO.setOuterTitle(question == null ? "内容已删除" : question.getTitle());
            }
            notificationDTO.setOuterid(notification.getOuterId());
            // 就地回复需要知道被回复的评论：评论类消息带 commentId，其余为 -1
            notificationDTO.setCommentId(notification.getCommentId());
            // 详情去掉与标题重复的部分，并截断过长的脏数据
            notificationDTO.setCommentContent(NotificationTextUtil.cleanDetail(
                    typeEnum, notification.getCommentContent(), notificationDTO.getOuterTitle()));
            notificationDTOS.add(notificationDTO);
        }

        PaginationDto<NotificationDTO> paginationDto = new PaginationDto<>();
        paginationDto.setData(notificationDTOS);
        // allCount 供前端分页显示「共 N 条」，不传会一直显示 0
        paginationDto.setAllCount(allNotification);
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;
    }

    @Override
    public NotificationDTO readNotification(String NotificationId, User user) {
        long id;
        try {
            id = Long.valueOf(NotificationId);
        } catch (Exception e) {
            return null;
        }
        Notification notification = notificationMapper.selectOneById(id);
        if (notification == null) {
            throw new CustomizeException(CustomizeErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (notification.getReceiver() != user.getId()) {
            throw new CustomizeException(CustomizeErrorCode.READ_NOTIFICATION_FAIL);
        }
        if (notification.getStatus() == 0) {
            notificationMapper.readNotification(id, user.getId());
        }
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setId(notification.getId());
        notificationDTO.setOuterid(notification.getOuterId());
        notificationDTO.setReceiver(notification.getReceiver());
        notificationDTO.setType(notification.getType());
        notificationDTO.setCommentId(notification.getCommentId());
        return notificationDTO;
    }

    @Override
    public long getNotificationAndTypeNumber(long receiver, int type) {
        return notificationMapper.getNotificationAndTypeNumber(receiver, type);
    }

    @Override
    public int readAll(long id) {
        return notificationMapper.readAll(id);
    }

    @Override
    public void createBatch(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        notificationMapper.insertBatch(notifications);
    }

    @Override
    public void createSystemNotification(long receiver, long notifier, long outerId, long commentId,
                                         int type, String content) {
        if (receiver <= 0) {
            return;
        }
        Notification notification = new Notification();
        notification.setCreateTime(System.currentTimeMillis());
        notification.setType(type);
        notification.setOuterId(outerId);
        notification.setCommentId(commentId);
        notification.setNotifier(notifier > 0 ? notifier : receiver);
        notification.setStatus(NotificationStatusEnum.UNREAD.getStatus());
        notification.setReceiver(receiver);
        notification.setCommentContent(content == null ? "" : content);
        notificationMapper.insertSelective(notification);
    }

    @Override
    public void notifyQuestionStatusChange(Question question, long notifierId,
                                           NotificationTypeEnum questionType, NotificationTypeEnum commentType) {
        // 作者收问题级通知，评论者收评论级通知；同一人既是作者又是评论者时只收问题级通知
        Map<Long, NotificationTypeEnum> receivers = new LinkedHashMap<>();
        receivers.put(question.getUserId(), questionType);
        // 只取评论者 ID，不加载评论正文
        for (Long commentator : commentMapper.selectDistinctCommentators(question.getQuestionId())) {
            if (commentator != null) {
                receivers.putIfAbsent(commentator, commentType);
            }
        }
        long now = System.currentTimeMillis();
        List<Notification> notifications = new ArrayList<>();
        receivers.forEach((receiver, type) -> {
            if (receiver == null || receiver == notifierId) {
                return;
            }
            Notification notification = new Notification();
            notification.setCreateTime(now);
            notification.setType(type.getType());
            notification.setOuterId(question.getQuestionId());
            notification.setNotifier(notifierId);
            notification.setStatus(NotificationStatusEnum.UNREAD.getStatus());
            notification.setReceiver(receiver);
            notifications.add(notification);
        });
        createBatch(notifications);
    }
}
