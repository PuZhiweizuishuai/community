package com.buguagaoshu.community.controller;


import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.FollowQuestion;
import com.buguagaoshu.community.model.FollowTopic;
import com.buguagaoshu.community.model.FollowUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.*;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-16 23:14
 */
@Controller
public class UserController {
    private final UserService userService;

    private final QuestionService questionService;

    private final FollowTopicService followTopicService;

    private final FollowUserService followUserService;

    private final FollowQuestionService followQuestionService;

    private final AdvertisementCache advertisementCache;

    @Autowired
    public UserController(UserService userService, QuestionService questionService, FollowTopicService followTopicService, FollowUserService followUserService, FollowQuestionService followQuestionService, AdvertisementCache advertisementCache) {
        this.userService = userService;
        this.questionService = questionService;
        this.followTopicService = followTopicService;
        this.followUserService = followUserService;
        this.followQuestionService = followQuestionService;
        this.advertisementCache = advertisementCache;
    }

    @GetMapping("/user/{userId}")
    public String getUserHome(@PathVariable("userId") String userId,
                              @RequestParam(value = "page", defaultValue = "1") String page,
                              @RequestParam(value = "size", defaultValue = "15") String size,
                              @RequestParam(value = "type", defaultValue = "0") String type,
                              Model model,
                              HttpServletRequest request) {
        if (userId == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        User user = userService.selectUserByUserId(userId);
        model.addAttribute("advertisements", advertisementCache.getUserHomeAdvertisementMap().values());

        if (user != null) {
            model.addAttribute("user", user);
            PaginationDto paginationDto = questionService.getQuestionByUserId(page, size, user.getId());
            model.addAttribute("paginationDto", paginationDto);
            model.addAttribute("type", type);
            User nowUser = (User) request.getSession().getAttribute("user");
            if (nowUser == null) {
                model.addAttribute("isFollowUser", true);
            } else {
                FollowUser followUser = new FollowUser();
                followUser.setFollowUserId(user.getId());
                followUser.setUserId(nowUser.getId());
                model.addAttribute("isFollowUser", followUserService.isFollowUser(followUser));
            }
            return StringUtil.jumpWebLangeParameter("user", false, request);
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }

    @GetMapping("/user/{userId}/following/topic")
    public String getUserFollowTopic(@PathVariable("userId") String userId,
                                     Model model,
                                     HttpServletRequest request) {
        if (userId == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        model.addAttribute("advertisements", advertisementCache.getUserHomeAdvertisementMap().values());

        User user = userService.selectUserByUserId(userId);
        if (user != null) {

            List<FollowTopic> followTopics = followTopicService.selectUserFollowTopic(user.getId());
            model.addAttribute("followTopics", followTopics);
            model.addAttribute("user", user);
            User nowUser = (User) request.getSession().getAttribute("user");
            if (nowUser == null) {
                model.addAttribute("isFollowUser", true);
            } else {
                FollowUser followUser = new FollowUser();
                followUser.setFollowUserId(user.getId());
                followUser.setUserId(nowUser.getId());
                model.addAttribute("isFollowUser", followUserService.isFollowUser(followUser));
            }
            return StringUtil.jumpWebLangeParameter("user", false, request);
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }

    @GetMapping("/user/{userId}/following/questions")
    public String getUserFollowQuestion(@PathVariable("userId") String userId,
                                        @RequestParam(value = "page", defaultValue = "1") String page,
                                        @RequestParam(value = "size", defaultValue = "20") String size,
                                        Model model,
                                        HttpServletRequest request) {
        if (userId == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        model.addAttribute("advertisements", advertisementCache.getUserHomeAdvertisementMap().values());

        User user = userService.selectUserByUserId(userId);
        if (user != null) {
            PaginationDto<FollowQuestion> followQuestionPaginationDto =  followQuestionService.selectUserFollowQuestion(user.getId(), page, size);
            model.addAttribute("followQuestions", followQuestionPaginationDto);
            model.addAttribute("user", user);
            User nowUser = (User) request.getSession().getAttribute("user");
            if (nowUser == null) {
                model.addAttribute("isFollowUser", true);
            } else {
                FollowUser followUser = new FollowUser();
                followUser.setFollowUserId(user.getId());
                followUser.setUserId(nowUser.getId());
                model.addAttribute("isFollowUser", followUserService.isFollowUser(followUser));
            }
            return StringUtil.jumpWebLangeParameter("user", false, request);
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }

    @GetMapping("/user/{userId}/following/user")
    public String getUserFollowUser(@PathVariable("userId") String userId,
                                    @RequestParam(value = "page", defaultValue = "1") String page,
                                    @RequestParam(value = "size", defaultValue = "20") String size,
                                    Model model,
                                    HttpServletRequest request) {
        if (userId == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        model.addAttribute("advertisements", advertisementCache.getUserHomeAdvertisementMap().values());

        User user = userService.selectUserByUserId(userId);
        if (user != null) {
            PaginationDto<User> userPaginationDto = followUserService.getFollowUserList(user.getId(), page, size);
            model.addAttribute("followUser", userPaginationDto);
            model.addAttribute("user", user);
            User nowUser = (User) request.getSession().getAttribute("user");
            if (nowUser == null) {
                model.addAttribute("isFollowUser", true);
            } else {
                FollowUser followUser = new FollowUser();
                followUser.setFollowUserId(user.getId());
                followUser.setUserId(nowUser.getId());
                model.addAttribute("isFollowUser", followUserService.isFollowUser(followUser));
            }
            return StringUtil.jumpWebLangeParameter("user", false, request);
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }


    @GetMapping("/user/{userId}/following/fans")
    public String getUserFans(@PathVariable("userId") String userId,
                              @RequestParam(value = "page", defaultValue = "1") String page,
                              @RequestParam(value = "size", defaultValue = "20") String size,
                              Model model,
                              HttpServletRequest request) {
        if (userId == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        model.addAttribute("advertisements", advertisementCache.getUserHomeAdvertisementMap().values());

        User user = userService.selectUserByUserId(userId);
        if (user != null) {
            PaginationDto<User> userPaginationDto = followUserService.getFansList(user.getId(), page, size);
            model.addAttribute("userFans", userPaginationDto);
            model.addAttribute("user", user);
            User nowUser = (User) request.getSession().getAttribute("user");
            if (nowUser == null) {
                model.addAttribute("isFollowUser", true);
            } else {
                FollowUser followUser = new FollowUser();
                followUser.setFollowUserId(user.getId());
                followUser.setUserId(nowUser.getId());
                model.addAttribute("isFollowUser", followUserService.isFollowUser(followUser));
            }
            return StringUtil.jumpWebLangeParameter("user", false, request);
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }
}
