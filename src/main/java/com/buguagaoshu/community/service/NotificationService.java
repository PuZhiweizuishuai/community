package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.model.Notification;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 13:15
 * 消息通知接口
 */
public interface NotificationService {
    /**
     * 获取该用户的所有通知
     * */
    long getAllNotificationNumber(long id);


    /**
     * 获取该用户的所有未读通知
     * */
    long getAllNotificationNoReadNumber(long id);


    /**
     * 返回通知列表
     * @param page 页码
     * @param size 大小
     * @param id 用户id
     * @return 通知
     * */
    PaginationDto<NotificationDTO> getAllNotification(String page, String size, long id, String type);


    /**
     * 设置消息已读
     * */
    NotificationDTO readNotification(String NotificationId, User user);


    long getNotificationAndTypeNumber(long receiver, int type);


    int readAll(long id);

    /**
     * 批量创建通知（空列表直接返回）
     */
    void createBatch(List<Notification> notifications);

    /**
     * 创建单条系统通知（接收人与发起人可以为同一人，例如「内容已提交审核」）。
     *
     * @param receiver  接收人
     * @param notifier  发起人（审核通过/驳回时为管理员，提交审核时为作者本人）
     * @param outerId   通知指向的问题 ID
     * @param commentId 通知指向的评论 ID，问题级通知传 -1
     * @param type      通知类型
     * @param content   消息摘要（驳回时为原因与修改意见）
     */
    void createSystemNotification(long receiver, long notifier, long outerId, long commentId,
                                  int type, String content);

    /**
     * 问题被删除/恢复时，批量通知作者与所有评论者（操作者本人不收通知，接收人去重）
     */
    void notifyQuestionStatusChange(Question question, long notifierId,
                                    NotificationTypeEnum questionType, NotificationTypeEnum commentType);
}
