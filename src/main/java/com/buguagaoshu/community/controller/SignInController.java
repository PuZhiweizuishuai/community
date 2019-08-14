package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.User;
import com.buguagaoshu.community.dto.UserPermission;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 13:39
 * 登陆控制
 */
@Controller
public class SignInController {
    /**
     * 返回登陆视图
     * */
    @GetMapping("/sign-in")
    public String signIn() {
        return "SignIn";
    }


    @RequestMapping(value = "/sign-in-controller", method = RequestMethod.POST)
    public String signInAndSkip(String email, String password, String remember,
                                Model model, HttpServletRequest request) {
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
            request.getSession().setAttribute("user", user);
            // 页面跳转
            return "redirect:/";
        } catch (UnknownAccountException e) {
            // UnknownAccountException 用户名不存在
            model.addAttribute("emailMsg", "用户不存在，请检查邮箱！");
            return "SignIn";
        } catch (IncorrectCredentialsException e2) {
            // IncorrectCredentialsException 密码错误
            model.addAttribute("pwdMsg", "密码错误！");
            return "SignIn";
        }
    }
}
