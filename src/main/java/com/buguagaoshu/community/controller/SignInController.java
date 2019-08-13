package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.util.StringUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 21:59
 * 登陆控制
 */
@RestController
public class SignInController {
    /**
     * 返回登陆视图
     * */
    @GetMapping("/sign-in")
    public ModelAndView signIn() {
        ModelAndView signInView = new ModelAndView();
        signInView.setViewName("SignIn");
        return signInView;
    }

    /**
     * 登陆接口
     * */
    @PostMapping("/api/signIn")
    @ResponseBody
    public HashMap<String, Object> judgeSignIn(String email, String password, String remember) {
        return StringUtil.dealResultMessage(false,email+"-" + password + "-" + remember);
    }
}
