package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.NotificationDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 22:52
 */
@Controller
public class MessageController {
    private final UserService userService;

    private final NotificationService notificationService;

    private final NotificationMapper notificationMapper;

    @Autowired
    public MessageController(UserService userService, NotificationService notificationService, NotificationMapper notificationMapper) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }
    /**
     * 负责跳转到个人信息页面
     * */
    @GetMapping("/message/{userId}")
    public String getMessagePage(@PathVariable("userId") String userId,
                                 @RequestParam(value = "page", defaultValue = "1") String page,
                                 @RequestParam(value = "size", defaultValue = "10") String size,
                                 @RequestParam(value = "type", defaultValue = "0") String type,
                                 Model model, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return StringUtil.jumpWebLangeParameter("/sign-in", true, request);
        } else if(!user.getUserId().equals(userId)) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        model.addAttribute("user", user);
        PaginationDto<NotificationDTO> paginationDto = notificationService.getAllNotification(page, size, user.getId(), type);
        model.addAttribute("notifications", paginationDto);

        model.addAttribute("commentCount", notificationMapper.getNoReadCommentCount(user.getId()));
        model.addAttribute("likeCount", notificationMapper.getNoReadLikeCount(user.getId()));
        model.addAttribute("systemCount", notificationMapper.getNoReadSystemCount(user.getId()));
        return "userMessage";
    }


    @GetMapping("/message/{userId}/{id}")
    public String readMessage(@PathVariable("userId") String userId,
                              @PathVariable("id") String id, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return StringUtil.jumpWebLangeParameter("/sign-in", true, request);
        }
        if(!user.getUserId().equals(userId)) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }

        NotificationDTO notificationDTO = notificationService.readNotification(id, user);

        System.out.println(notificationDTO.getOuterid());

        if(notificationDTO.getReceiver() == user.getId()) {
            return StringUtil.jumpWebLangeParameter("/question/" + notificationDTO.getOuterid(), true, request);
        } else {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
    }


    /**
     * 一键已读
     * */
    @GetMapping("/message/{userId}/readAll")
    public String readAll(@PathVariable("userId") String userId, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return StringUtil.jumpWebLangeParameter("/sign-in", true, request);
        }
        if(!user.getUserId().equals(userId)) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        if((Long) request.getSession().getAttribute("notificationCount") == 0) {
            return StringUtil.jumpWebLangeParameter("/message/" + user.getUserId(), true, request);
        }
        notificationService.readAll(user.getId());
        return StringUtil.jumpWebLangeParameter("/message/" + user.getUserId(), true, request);
    }
}
