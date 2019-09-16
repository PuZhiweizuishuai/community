package com.buguagaoshu.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-16 21:10
 */
@Controller
public class AboutMeController {
    @GetMapping("/about")
    public String getAboutMePage() {
        return "aboutMe";
    }
}
