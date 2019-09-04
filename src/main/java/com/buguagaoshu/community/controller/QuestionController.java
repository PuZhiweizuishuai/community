package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-20 21:32
 */
@Controller
public class QuestionController {
    private final QuestionService questionService;

    private final CommentService commentService;

    @Autowired
    public QuestionController(QuestionService questionService, CommentService commentService) {
        this.questionService = questionService;
        this.commentService = commentService;
    }

    @GetMapping("/question/{questionId}")
    public String getQuestion(@PathVariable("questionId") String questionId,
                              @RequestParam(value = "page", defaultValue = "1") String page,
                              @RequestParam(value = "size", defaultValue = "10") String size,
                              Model model,
                              HttpServletRequest request) {
        QuestionDto question = questionService.selectQuestionById(questionId);
        if (question == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }

        List<Question> relevantQuestion = questionService.getRelevantQuestion(question);
        PaginationDto<CommentDto> commentDtos = commentService.getCommentDtoByQuestionIdForQuestion(questionId, page, size);
        // TODO 阅读数加1，此处待优化，如限定一个ip只能增加一次阅读数
        questionService.updateQuestionViewCount(question.getQuestionId());

        model.addAttribute("question", question);
        model.addAttribute("comments", commentDtos);
        model.addAttribute("relevantQuestion", relevantQuestion);
        return "question";
    }
}
