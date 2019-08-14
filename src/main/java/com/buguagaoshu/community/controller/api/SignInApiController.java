package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 21:59
 * 登陆控制API
 */
@RestController
public class SignInApiController {
    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final OnlineUserService onlineUserService;

    @Autowired
    public SignInApiController(UserPermissionService userPermissionService, UserService userService, OnlineUserService onlineUserService) {
        this.userPermissionService = userPermissionService;
        this.userService = userService;
        this.onlineUserService = onlineUserService;
    }



    /**
     * 登陆接口
     * */
    @PostMapping("/api/signIn")
    @ResponseBody
    public HashMap<String, Object> judgeSignIn(String email, String password,
                                               String remember, HttpServletRequest request) {
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
            // 查找用户权限
            UserPermission userPermission =userPermissionService.selectUserPermissionById(user.getId());
            // 写入登陆时间
            userService.updateUserLastTimeById(user.getId(), StringUtil.getNowTime());
            // 写入在线表
            onlineUserService.insertOnlineUser(new OnlineUser(user.getId(), user.getUserName(), user.getPassword(), IpUtil.getIpAddr(request), StringUtil.getNowTime()));
            HashMap<String, Object> hashMap = new HashMap<>(3);
            hashMap.put("msg", true);
            hashMap.put("user",user);
            // 加入 token
            hashMap.put("token", user.getPassword());
            hashMap.put("userPermission", userPermission.getPower());

            user.setPassword("保密");
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
