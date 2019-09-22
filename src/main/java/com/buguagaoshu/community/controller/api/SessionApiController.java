package com.buguagaoshu.community.controller.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-22 22:57
 * 学习使用 spring-session-jdbc
 */
@RestController
@RequestMapping("/api/session")
public class SessionApiController {
    private static final String SESSION = "session";

    @GetMapping
    public String session(HttpSession httpSession) {
        Object session = httpSession.getAttribute(SESSION);
        if(session == null) {
            httpSession.setAttribute(SESSION, SESSION);
        }
        return (String) httpSession.getAttribute(SESSION);
    }
}
