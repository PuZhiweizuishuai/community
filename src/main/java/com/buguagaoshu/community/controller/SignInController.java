package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SignInService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import com.wf.captcha.utils.CaptchaUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 13:39
 * 登陆控制
 */
@Controller
public class SignInController {
    private final SignInService signInService;

    @Autowired
    public SignInController(SignInService signInService) {
        this.signInService = signInService;
    }

    /**
     * 返回登陆视图
     */
    @GetMapping("/sign-in")
    public String signIn(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return "SignIn";
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }


    @RequestMapping(value = "/sign-in", method = RequestMethod.POST)
    public String signInAndSkip(String email, String password, String remember, String validateCode,
                                Model model, HttpServletRequest request, HttpServletResponse response) {
        try {
            HashMap<String, Object> signInMsg = signInService.signIn(email, password, remember, validateCode, request);
            User user = (User) signInMsg.get("user");
            // 设置 session
            request.getSession().setAttribute("user", user);
            // 写入 cookie
            Cookie cookie = new Cookie("token", (String) signInMsg.get("token"));
            response.addCookie(cookie);
            return StringUtil.jumpWebLangeParameter("/", true, request);
        } catch (CustomizeException e) {
            if (CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR.getCode().equals(e.getCode())) {
                CaptchaUtil.clear(request);
                model.addAttribute("kaptchaMsg", "验证码错误！");
            } else if (CustomizeErrorCode.SIGN_IN_EMAIL_ERROR.getCode().equals(e.getCode())) {
                model.addAttribute("emailMsg", "用户不存在，请检查邮箱！");
            } else if (CustomizeErrorCode.SIGN_IN_EMAIL_OR_PASSWORD_NULL.getCode().equals(e.getCode())) {
                model.addAttribute("emailMsg", "密码和邮箱不能为空");
            } else if (CustomizeErrorCode.SIGN_IN_PASSWORD_ERROR.getCode().equals(e.getCode())) {
                model.addAttribute("pwdMsg", "密码错误！");
            }
            return StringUtil.jumpWebLangeParameter("SignIn", false, request);
        }
    }
}
