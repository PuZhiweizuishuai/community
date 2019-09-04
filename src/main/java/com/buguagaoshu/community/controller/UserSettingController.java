package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import javax.servlet.http.HttpServletRequest;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 21:53
 */
@Controller
public class UserSettingController {
    @GetMapping("/update/head")
    public String getHeadPage(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return StringUtil.jumpWebLangeParameter("/sign-in", true, request);
        }
        return "head";
    }


    @GetMapping("/setting")
    public String getSettingPage(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return StringUtil.jumpWebLangeParameter("/sign-in", true, request);
        }
        return "userSetting";
    }
}
