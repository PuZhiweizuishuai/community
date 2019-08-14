package com.buguagaoshu.community.controller;

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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 13:39
 * 登陆控制
 */
@Controller
public class SignInController {
    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final OnlineUserService onlineUserService;

    @Autowired
    public SignInController(UserService userService, UserPermissionService userPermissionService, OnlineUserService onlineUserService) {
        this.userService = userService;
        this.userPermissionService = userPermissionService;
        this.onlineUserService = onlineUserService;
    }

    /**
     * 返回登陆视图
     * */
    @GetMapping("/sign-in")
    public String signIn() {
        return "SignIn";
    }


    @RequestMapping(value = "/sign-in-controller", method = RequestMethod.POST)
    public String signInAndSkip(String email, String password, String remember,
                                Model model, HttpServletRequest request, HttpServletResponse response) {
        if(email == null && password == null) {
            model.addAttribute("emailMsg", "密码和邮箱不能为空");
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
            // 写入权限
            UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
            user.setPower(userPermission.getPower());
            // 写入在线表
            onlineUserService.insertOnlineUser(new OnlineUser(user.getId(), user.getUserName(), user.getPassword(), IpUtil.getIpAddr(request), StringUtil.getNowTime()));
            //写入 cookie
            response.addCookie(new Cookie("token", user.getPassword()));

            user.setPassword("保密");

            // 更新登陆时间
            userService.updateUserLastTimeById(user.getId(), StringUtil.getNowTime());
            // 页面跳转
            return StringUtil.jumpWebLangeParameter("/", true, request);
        } catch (UnknownAccountException e) {
            // UnknownAccountException 用户名不存在
            model.addAttribute("emailMsg", "用户不存在，请检查邮箱！");
            return StringUtil.jumpWebLangeParameter("SignIn", false, request);
        } catch (IncorrectCredentialsException e2) {
            // IncorrectCredentialsException 密码错误
            model.addAttribute("pwdMsg", "密码错误！");
            return StringUtil.jumpWebLangeParameter("SignIn", false, request);
        }
    }
}
