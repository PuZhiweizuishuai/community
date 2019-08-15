package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页控制类
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create 2019-08-12 16:06
 */
@Controller
public class IndexController {
    private final OnlineUserService onlineUserService;

    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final QuestionService questionService;

    @Autowired
    public IndexController(OnlineUserService onlineUserService, UserService userService,
                           UserPermissionService userPermissionService, QuestionService questionService) {
        this.onlineUserService = onlineUserService;
        this.userService = userService;
        this.userPermissionService = userPermissionService;
        this.questionService = questionService;
    }

    @GetMapping(value = {"/", "index"})
    public String index(HttpServletRequest request, Model model) {
        /**
         * TODO 待改进优化
         * */
        String token = null;
        if(request.getCookies() != null) {
            Cookie[] cookies = request.getCookies();
            if(cookies != null) {
                for(Cookie cookie : cookies) {
                    if(cookie.getName().equals("token")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

        }
        if(token != null) {
            OnlineUser onlineUser = onlineUserService.selectOnlineUserByToken(token);
            if(onlineUser != null) {
                User user = userService.selectUserById(onlineUser.getId());
                UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
                user.setPower(userPermission.getPower());
                request.getSession().setAttribute("user", user);
            }

        }

        List<QuestionDto> questionDtoList = questionService.getSomeQuestionDto();
        model.addAttribute("questions", questionDtoList);
        System.out.println(questionDtoList == null);
        return "index";
    }
}
