package com.buguagaoshu.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 21:53
 */
@Controller
public class HeadController {
    @GetMapping("/head")
    public String getHeadPage() {
        return "head";
    }


    @PostMapping("/head")
    @ResponseBody
    public Object post(String imgBase) {
        System.out.println(imgBase);
        HashMap<String, Boolean> hashMap = new HashMap<>(2);
        hashMap.put("ret", true);
        return hashMap;
    }
}
