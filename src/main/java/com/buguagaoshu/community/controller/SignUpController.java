package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 13:41
 * 注册控制
 */
@Controller
public class SignUpController {
    @GetMapping("/sign-up")
    public String signUp(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return "SignUp";
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }

    public String getSignUp() {
        return null;
    }
}
