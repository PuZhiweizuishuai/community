package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.captcha.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-22 13:51
 * 输出验证码
 */
@Controller
@Slf4j
public class CaptchaController {
    @GetMapping(value = "/captche/images")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CaptchaUtil.out(request, response);
    }
}
