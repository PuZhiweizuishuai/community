package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.cache.TagCache;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.wf.captcha.utils.CaptchaUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 0:17
 */
@Controller
public class PublishController {
    private final QuestionService questionService;

    @Autowired
    public PublishController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/publish")
    public String getPublishPage(Model model, HttpServletRequest request) {
        /**
         * TODO 简单验证，后期改为JWT 方便测试页面，先注释
         * */
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return StringUtil.jumpWebLangeParameter("/sign-in", true, request);
        }

        model.addAttribute("question", new Question());
        return "publish";
    }

    /**
     * 发帖
     */
    @PostMapping("/publish")
    public String publishPost(@RequestParam("title") String title,
                              @RequestParam("classification") String classification,
                              @RequestParam("description") String description,
                              //@RequestParam("issuesFile") MultipartFile file,
                              @RequestParam("tag") String tag,
                              @RequestParam(value = "questionId", defaultValue = "-1") String questionId,
                              @RequestParam("CAPTCHA") String CAPTCHA,
                              HttpServletRequest request,
                              Model model) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return StringUtil.jumpWebLangeParameter("/SignIn", true, request);
        }
        long id;
        try {
            id = Long.valueOf(questionId);
        } catch (Exception e) {
            id = -1;
        }
        Question question = new Question();
        question.setQuestionId(id);
        question.setTitle(title);
        question.setClassification(classification);
        question.setDescription(description);
        question.setTag(tag);
        question.setUserId(user.getId());

        if (!check(question, CAPTCHA, model, request)) {
            return StringUtil.jumpWebLangeParameter("publish", false, request);
        }

        questionService.createQuestion(question);
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }

    /**
     * 编辑帖子
     */
    @GetMapping("/publish/{questionId}")
    public String editQuestion(@PathVariable("questionId") String questionId,
                               HttpServletRequest request, Model model) {

        User user = (User) request.getSession().getAttribute("user");
        Question question = questionService.selectQuestionNotDtoById(questionId);
        if (user != null && question != null && user.getId() == question.getUserId()) {
            model.addAttribute("question", question);
            return StringUtil.jumpWebLangeParameter("publish", false, request);
        }
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }

    private boolean check(Question question, String CAPTCHA, Model model, HttpServletRequest request) {

        boolean isTag = StringUtil.judgeTagNumber(question.getTag());
        if (question.getTitle() != null && !question.getTitle().equals("")
                && question.getClassification() != null && !question.getClassification().equals("null")
                && CaptchaUtil.ver(CAPTCHA, request)
                && question.getDescription() != null && !question.getDescription().equals("") && isTag) {
            CaptchaUtil.clear(request);
            return true;
        }



        if (question.getTitle() == null || question.getTitle().equals("")) {
            model.addAttribute("titleMessage", "标题不能为空！");
        }
        if (question.getClassification() == null || question.getClassification().equals("null")) {
            model.addAttribute("classMessage", "分类不能为空！");
        }
        if (question.getDescription() == null || question.getDescription().equals("")) {
            model.addAttribute("textMessage", "内容不能为空！");
        }
        if (!CaptchaUtil.ver(CAPTCHA, request)) {
            CaptchaUtil.clear(request);
            model.addAttribute("CAPTCHAMessage", "验证码错误！");
        }
        if (!isTag) {
            model.addAttribute("tagMessage", "最多只能输入6个标签！");
        }
        model.addAttribute("question", question);
        return false;
    }
}
