package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 23:01
 */
@Controller
public class SignOutController {
    private final OnlineUserService onlineUserService;

    @Autowired
    public SignOutController(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }


    /**
     * TODO 需要优化的退出方法
     * 暂时实现的退出方法
     */
    @RequestMapping(value = "sign-out", method = RequestMethod.GET)
    public String signOut(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("token")) {
                    cookie.setValue(null);
                    break;
                }
            }
        }
        onlineUserService.deleteOnlineUserById(user.getId());
        request.getSession().removeAttribute("user");
        return "redirect:/";
    }
}
