package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.util.IpUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
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
    /**
     * 返回该用户当前访问的 ip
     * */
    @GetMapping("/api/getUserIp")
    @ResponseBody
    public String getUserIP(HttpServletRequest httpsServer) {
        return IpUtil.getIpAddr(httpsServer);
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
