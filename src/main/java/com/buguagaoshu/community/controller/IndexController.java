package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.PaginationDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String index(HttpServletRequest request, Model model,
                        @RequestParam(value = "page", defaultValue = "1") String page,
                        @RequestParam(value = "size", defaultValue = "10") String size) {
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

        model.addAttribute("offline", false);
        if(token != null) {
            OnlineUser onlineUser = onlineUserService.selectOnlineUserByToken(token);
            if(onlineUser != null) {
                User user = userService.selectUserById(onlineUser.getId());
                UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
                user.setPower(userPermission.getPower());
                request.getSession().setAttribute("user", user);
            } else {
                request.getSession().setAttribute("user", null);
                model.addAttribute("offline", true);
            }
        }

        PaginationDto paginationDto = questionService.getSomeQuestionDto(page, size);
        model.addAttribute("paginationDto", paginationDto);
        return "index";
    }
}
