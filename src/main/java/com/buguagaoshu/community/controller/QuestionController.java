package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.enums.CommentSortTypeEnum;
import com.buguagaoshu.community.model.ClickLike;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ClickLikeService;
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

    private final ClickLikeService clickLikeService;

    @Autowired
    public QuestionController(QuestionService questionService, CommentService commentService, ClickLikeService clickLikeService) {
        this.questionService = questionService;
        this.commentService = commentService;
        this.clickLikeService = clickLikeService;
    }

    @GetMapping("/question/{questionId}")
    public String getQuestion(@PathVariable("questionId") String questionId,
                              @RequestParam(value = "page", defaultValue = "1") String page,
                              @RequestParam(value = "size", defaultValue = "10") String size,
                              @RequestParam(value = "sort", defaultValue = "0") String sort,
                              Model model,
                              HttpServletRequest request) {
        QuestionDto question = questionService.selectQuestionById(questionId);
        if (question == null) {
            return StringUtil.jumpWebLangeParameter("/", true, request);
        }


        if (question.getStatus() == 0) {
            User user = (User) request.getSession().getAttribute("admin");
            if (user == null) {
                model.addAttribute("message", "这个问题已经被删除了！如需恢复，请联系管理员！");
                return "error";
            }
            List<Question> relevantQuestion = questionService.getRelevantQuestion(question);
            PaginationDto<CommentDto> commentDtos = commentService.getCommentDtoByQuestionIdForQuestion(questionId, page, size, CommentSortTypeEnum.getType(sort));
            model.addAttribute("question", question);
            model.addAttribute("comments", commentDtos);
            model.addAttribute("relevantQuestion", relevantQuestion);
            model.addAttribute("sort", sort);
            return "question";
        }

        List<Question> relevantQuestion = questionService.getRelevantQuestion(question);
        PaginationDto<CommentDto> commentDtos = commentService.getCommentDtoByQuestionIdForQuestion(questionId, page, size, CommentSortTypeEnum.getType(sort));
        // TODO 阅读数加1，此处待优化，如限定一个ip只能增加一次阅读数
        questionService.updateQuestionViewCount(question.getQuestionId());

        // 判断点赞
        User user = (User) request.getSession().getAttribute("user");
        ClickLike clickLike = new ClickLike();
        Boolean isQuestionClickLike = false;
        if (user == null) {
            isQuestionClickLike = false;

        } else {
            clickLike.setNotifier(user.getId());
            clickLike.setQuestionId(question.getQuestionId());
            clickLike.setCommentId(-1);
            if (clickLikeService.isClickLikeQuestion(clickLike)) {
                isQuestionClickLike = true;
            } else {
                isQuestionClickLike = false;
            }
        }
        model.addAttribute("isQuestionClickLike", isQuestionClickLike);
        model.addAttribute("question", question);
        model.addAttribute("comments", commentDtos);
        model.addAttribute("relevantQuestion", relevantQuestion);
        model.addAttribute("sort", sort);
        return "question";
    }
}
