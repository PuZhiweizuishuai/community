package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 15:22
 * 一些工具栏 api 接口
 */
@RestController
public class ApiUtilController {
    private final UserService userService;

    @Autowired
    public ApiUtilController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 返回该用户当前访问的 ip
     */
    @GetMapping("/api/getUserIp")
    @ResponseBody
    public String getUserIP(HttpServletRequest httpsServer) {
        return IpUtil.getIpAddr(httpsServer);
    }


    /**
     * 检查邮箱
     */
    @GetMapping("/api/checkEmail")
    @ResponseBody
    public HashMap<String, Object> checkEmail(String email) {
        if (!StringUtil.checkEmail(email)) {
            return StringUtil.dealResultMessage(false, "该邮箱格式错误！");
        }
        if (userService.selectUserByEmail(email) != null) {
            return StringUtil.dealResultMessage(false, "该邮箱已被注册，请重新输入，或登陆");
        } else {
            return StringUtil.dealResultMessage(true, "该邮箱已可用");
        }
    }


    /**
     * 检查用户名重复
     */
    @GetMapping("/api/checkUserId")
    @ResponseBody
    public HashMap<String, Object> checkUserId(String userId) {
        if (!StringUtil.checkUserId(userId)) {
            return StringUtil.dealResultMessage(false, "该账号格式错误！");
        }
        if (userService.selectUserByUserId(userId) != null) {
            return StringUtil.dealResultMessage(false, "该账号已被注册，请重新输入,或更换账号！");
        } else {
            return StringUtil.dealResultMessage(true, "该账号可用");
        }
    }

    /*
    @GetMapping("/api/getShiroSession")
    public HashMap<String, Object> getShiroSession() {
        Subject subject = SecurityUtils.getSubject();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("user", subject.getSession().getAttribute("user"));
        return hashMap;
    }
    */
}
