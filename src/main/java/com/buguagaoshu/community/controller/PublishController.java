package com.buguagaoshu.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 0:17
 */
@Controller
public class PublishController {
    @GetMapping("/publish")
    public String getPublishPage() {
        return "publish";
    }
}
