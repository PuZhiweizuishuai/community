package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.util.IpUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 15:22
 * 一些工具栏 api 接口
 */
@RestController
public class ApiUtilController {
    @GetMapping("/api/getUserIp")
    @ResponseBody
    public String getUserIP(HttpServletRequest httpsServer) {
        return IpUtil.getIpAddr(httpsServer);
    }
}
