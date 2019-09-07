package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;

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
}
