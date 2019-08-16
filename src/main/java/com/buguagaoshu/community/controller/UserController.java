package com.buguagaoshu.community.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-16 23:14
 */
@Controller
public class UserController {
    @GetMapping("/user")
    public String getUserHome() {
        return "user";
    }
}
