package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-20 21:32
 */
@Controller
public class QuestionController {
    private final QuestionService questionService;

    @Autowired
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/question/{questionId}")
    public String getQuestion(@PathVariable("questionId") String questionId,
                              Model model,
                              HttpServletRequest request) {
        QuestionDto question = questionService.selectQuestionById(questionId);
        if(question == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }
        model.addAttribute("question", question);


        return "question";
    }
}
