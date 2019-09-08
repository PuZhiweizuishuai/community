package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.cache.IndexTopQuestion;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.enums.QuestionClassType;
import com.buguagaoshu.community.enums.QuestionSortType;
import com.buguagaoshu.community.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * 首页控制类
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create 2019-08-12 16:06
 */
@Controller
public class IndexController {

    private final QuestionService questionService;

    private final HotTagCache hotTagCache;

    private final IndexTopQuestion indexTopQuestion;

    @Autowired
    public IndexController(QuestionService questionService, HotTagCache hotTagCache, IndexTopQuestion indexTopQuestion) {
        this.questionService = questionService;
        this.hotTagCache = hotTagCache;
        this.indexTopQuestion = indexTopQuestion;
    }


    @GetMapping(value = {"/", "index"})
    public String index(HttpServletRequest request, Model model,
                        @RequestParam(value = "page", defaultValue = "1") String page,
                        @RequestParam(value = "size", defaultValue = "10") String size,
                        @RequestParam(value = "tag", required = false) String tag,
                        @RequestParam(value = "sort", defaultValue = "3") String sort,
                        @RequestParam(value = "class", defaultValue = "0") String classification) {
        List<String> hots = hotTagCache.getHots();
        Integer s;
        Integer c;
        try {
            s = Integer.valueOf(sort);
        } catch (Exception e) {
            s = QuestionSortType.NEW_QUESTION.getType();
        }
        try {
            c = Integer.valueOf(classification);
        } catch (Exception e) {
            c = QuestionClassType.ALL.getType();
        }
        // 首页置顶问题显示
        if (page.equals("1") && (tag == null || tag.equals("")) && sort.equals("3") && classification.equals("0")) {
            // Long[] question = new Long[]{23L};
            // indexTopQuestion.setTopQuestion(question);
            model.addAttribute("TopQuestion", indexTopQuestion.getTopQuestion());
        }

        PaginationDto<QuestionDto> paginationDto = questionService.getSomeQuestionDto(page, size, tag, s, c);
        model.addAttribute("paginationDto", paginationDto);
        model.addAttribute("hots", hots);
        model.addAttribute("tag", tag);
        model.addAttribute("sort", sort);
        model.addAttribute("class", classification);
        return "index";
    }
}
