package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.cache.HotQuestionCache;
import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.service.TagClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-16 21:56
 */
@Controller
public class ClassPageController {
    private final HotTagCache hotTagCache;

    private final HotQuestionCache hotQuestionCache;

    private final TagClassService tagClassService;

    @Autowired
    public ClassPageController(HotTagCache hotTagCache, HotQuestionCache hotQuestionCache, TagClassService tagClassService) {
        this.hotTagCache = hotTagCache;
        this.hotQuestionCache = hotQuestionCache;
        this.tagClassService = tagClassService;
    }

    @GetMapping("/class")
    public String getClassPage(Model model, @RequestParam(value = "type", defaultValue = "1") String type) {
        model.addAttribute("hotQuestions", hotQuestionCache.getHotQuestionDTOList());
        List<String> hots = hotTagCache.getHots();
        model.addAttribute("hots", hots);
        model.addAttribute("tags", tagClassService.getTagClassByTypeNotUserPage(type));
        model.addAttribute("type", type);
        return "class";
    }
}
