package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.User;
import com.buguagaoshu.community.dto.UserPermission;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 21:59
 * 登陆控制API
 */
@RestController
public class SignInApiController {
    private final UserPermissionService userPermissionService;

    @Autowired
    public SignInApiController(UserPermissionService userPermissionService) {
        this.userPermissionService = userPermissionService;
    }



    /**
     * 登陆接口
     * */
    @PostMapping("/api/signIn")
    @ResponseBody
    public HashMap<String, Object> judgeSignIn(String email, String password, String remember) {
        if(email == null && password == null) {
            return StringUtil.dealResultMessage(false, "用户名和密码为空！");
        }
        boolean rememberMe = false;
        if(remember != null) {
            rememberMe = true;
        }
        // 获取 Subject
        Subject subject = SecurityUtils.getSubject();
        // 封装用户数据
        UsernamePasswordToken token = new UsernamePasswordToken(email, password, rememberMe);
        // 执行登陆方法
        try {
            // 没有异常就是登陆成功
            subject.login(token);
            User user = (User) subject.getPrincipal();
            UserPermission userPermission =userPermissionService.selectUserPermissionById(user.getId());
            HashMap<String, Object> hashMap = new HashMap<>(3);
            hashMap.put("msg", true);
            hashMap.put("user",user);
            hashMap.put("userPermission", userPermission.getPower());
            return hashMap;
        } catch (UnknownAccountException e) {
            // UnknownAccountException 用户名不存在
            return StringUtil.dealResultMessage(false, "用户不存在，请检查邮箱！");
        } catch (IncorrectCredentialsException e2) {
            // IncorrectCredentialsException 密码错误
            return StringUtil.dealResultMessage(false, "密码错误！");
        }
    }
}
