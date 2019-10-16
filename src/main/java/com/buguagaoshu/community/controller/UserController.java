package com.buguagaoshu.community.controller;


import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
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
 * create          2019-08-16 23:14
 */
@Controller
public class UserController {
    private final UserService userService;

    private final QuestionService questionService;

    @Autowired
    public UserController(UserService userService, QuestionService questionService) {
        this.userService = userService;
        this.questionService = questionService;
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
        if (user != null) {
            model.addAttribute("user", user);
            PaginationDto paginationDto = questionService.getQuestionByUserId(page, size, user.getId());
            model.addAttribute("paginationDto", paginationDto);
            model.addAttribute("type", type);
            return StringUtil.jumpWebLangeParameter("user", false, request);
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }
}
