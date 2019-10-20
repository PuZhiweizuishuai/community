package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.VditorDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 18:06
 */
@Controller
public class TestController {
    @GetMapping("/edit")
    public String test() {
        return "markdown/edit";
    }


    @GetMapping("/show")
    public String show() {
        return "admin/index";
    }

    @PostMapping("/api/upload/editor")
    @ResponseBody
    public Map<String, Object> upload(@RequestParam(value = "file[]", required = false) MultipartFile[] files) {

        System.out.println(files.length);
        System.out.println(files[0].getOriginalFilename());

        Map<String, Object> map = new HashMap<>(4);
        map.put("msg", "上传失败，请重试！");
        map.put("code", 1);

        Map<String, Object> succMap = new HashMap<>(2);
        succMap.put("360截图-32277531.jpg", "/image/101-desktop-wallpaper.png");

        Map<String, Object> data = new HashMap<>(2);
        String[] err = new String[0];//{"360截图-32277531.jpg"};
        data.put("errFiles", err);
        data.put("succMap", succMap);

        map.put("data", data);

        return  map;
    }

}
