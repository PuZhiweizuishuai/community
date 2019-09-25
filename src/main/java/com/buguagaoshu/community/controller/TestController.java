package com.buguagaoshu.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 18:06
 */
@Controller
public class TestController {
    @GetMapping("/test")
    public String test() {
        return "test";
    }
}
