package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.Notification;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 13:17
 */
@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;

    private final UserMapper userMapper;


    private final QuestionMapper questionMapper;

    @Autowired
    public NotificationServiceImpl(NotificationMapper notificationMapper, UserMapper userMapper,
                                   QuestionMapper questionMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.questionMapper = questionMapper;
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

        Set<Long> disUserIds = notifications.stream().map(notify -> notify.getNotifier()).collect(Collectors.toSet());

        List<NotificationDTO> notificationDTOS = new ArrayList<>();


        List<User> users = new ArrayList<>();
        for (Long userId : disUserIds) {
            User user = userMapper.selectUserById(userId);
            users.add(user);
        }
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(u -> u.getId(), u -> u));

        for (Notification notification : notifications) {
            NotificationDTO notificationDTO = new NotificationDTO();

            notificationDTO.setId(notification.getId());
            notificationDTO.setCreateTime(StringUtil.foematTime(notification.getCreateTime()));

            notificationDTO.setNotifier(notification.getNotifier());
            notificationDTO.setNotifierId(userMap.get(notification.getNotifier()).getUserId());
            notificationDTO.setNotifierName(userMap.get(notification.getNotifier()).getUserName());

            Question question = questionMapper.getQuestionIgnoreStatus(notification.getOuterId());
            //TODO 目测可以优化
            notificationDTO.setOuterTitle(question.getTitle());
            notificationDTO.setOuterid(notification.getOuterId());
            notificationDTO.setTypeName(NotificationTypeEnum.nameOfType(notification.getType()));
            notificationDTO.setType(notification.getType());
            notificationDTO.setStatus(notification.getStatus());
            notificationDTOS.add(notificationDTO);
        }

        PaginationDto<NotificationDTO> paginationDto = new PaginationDto<>();
        paginationDto.setData(notificationDTOS);
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
        Notification notification = notificationMapper.selectNotificationById(id);
        if (notification == null) {
            throw new CustomizeException(CustomizeErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (notification.getReceiver() != user.getId()) {
            throw new CustomizeException(CustomizeErrorCode.READ_NOTIFICATION_FAIL);
        }
        if (notification.getStatus() == 0) {
            notificationMapper.readNotification(id);
        }
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setId(notification.getId());
        notificationDTO.setOuterid(notification.getOuterId());
        notificationDTO.setReceiver(notification.getReceiver());
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
}
