package com.buguagaoshu.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 13:41
 * 注册控制
 */
@Controller
public class SignUpController {
    @GetMapping("/sign-up")
    public String signUp() {
        return "SignUp";
    }

    public String getSignUp() {
        return null;
    }
}
