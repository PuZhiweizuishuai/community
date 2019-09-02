package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 22:52
 */
@Controller
public class MessageController {
    final
    UserService userService;

    @Autowired
    public MessageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/message/{userId}")
    public String getMessagePage(@PathVariable("userId") String userId, Model model,
                                 HttpServletRequest request) {
        /*
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return StringUtil.jumpWebLangeParameter("/sign-in", true, request);
        } else if(!user.getUserId().equals(userId)) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        */
        model.addAttribute("user", userService.selectUserByUserId(userId));
        return "userMessage";
    }
}
