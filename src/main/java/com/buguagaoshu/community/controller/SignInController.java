package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.User;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
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
        System.out.println(remember);
        User user = userService.selectUserByEmail(email);
        if(user != null) {
            if(StringUtil.judgePassword(password, user.getPassword())) {
                // TODO 判断是否记住密码
                user.setPassword(null);
                HashMap<String, Object> longUserHashMap = new HashMap<>(2);
                longUserHashMap.put("msg", true);
                longUserHashMap.put(email, user);
                // TODO 页面跳转
                return longUserHashMap;
            } else {
                return StringUtil.dealResultMessage(false, "密码错误！");
            }
        } else {
            return StringUtil.dealResultMessage(false, "用户不存在，请检查邮箱！");
        }
    }
}
