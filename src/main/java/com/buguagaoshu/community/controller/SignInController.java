package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.User;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final UserService userService;

    @Autowired
    public SignInController(UserService userService) {
        this.userService = userService;
    }

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
        // 获取 Subject
        Subject subject = SecurityUtils.getSubject();

        // 封装用户数据
        UsernamePasswordToken token = new UsernamePasswordToken(email, password);

        // 执行登陆方法
        try {
            // 没有异常就是登陆成功
            subject.login(token);
            return StringUtil.dealResultMessage(true, "chengggeng");
        } catch (UnknownAccountException e) {
            // UnknownAccountException 用户名不存在
            return StringUtil.dealResultMessage(false, "用户不存在，请检查邮箱！");
        } catch (IncorrectCredentialsException e2) {
            // IncorrectCredentialsException 密码错误
            return StringUtil.dealResultMessage(false, "密码错误！");
        }



    }
}
