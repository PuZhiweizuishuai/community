package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.StringUtil;
import com.google.code.kaptcha.Constants;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public String getPublishPage(Model model) {
        model.addAttribute("question", new Question());
        return "publish";
    }

    /**
     * 发帖
     * */
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
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        question.setUserId(user.getId());
        question.setCreateTime(StringUtil.getNowTime());
        question.setAlterTime(StringUtil.getNowTime());


        if(!check(question, CAPTCHA, model)) {
            return StringUtil.jumpWebLangeParameter("publish", false, request);
        }

        questionService.createQuestion(question);
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }

    /**
     * 编辑帖子
     * */
    @GetMapping("/publish/{questionId}")
    public String editQuestion(@PathVariable("questionId") String questionId,
                               HttpServletRequest request, Model model) {

        User user = (User) request.getSession().getAttribute("user");
        Question question = questionService.selectQuestionNotDtoById(questionId);
        if(user != null && question != null && user.getId() == question.getUserId()) {
            model.addAttribute("question", question);
            return StringUtil.jumpWebLangeParameter("publish",false, request);
        }
        return StringUtil.jumpWebLangeParameter("/",true, request);
    }

    private boolean check(Question question, String CAPTCHA ,Model model) {
        if(!question.getTitle().isEmpty() && !question.getClassification().isEmpty() && checkEmpty(CAPTCHA)
                && !question.getDescription().isEmpty()) {
            return true;
        }
        if(question.getTitle().isEmpty()) {
            model.addAttribute("titleMessage", "标题不能为空！");
        }
        if(question.getClassification().isEmpty()) {
            model.addAttribute("classMessage", "分类不能为空！");
        }
        if(question.getDescription().isEmpty()) {
            model.addAttribute("textMessage", "内容不能为空！");
        }
        if(!checkEmpty(CAPTCHA)) {
            model.addAttribute("CAPTCHAMessage","验证码错误");
        }
        model.addAttribute("question", question);
        return false;
    }

    public boolean checkEmpty(String CAPTCHA) {
        if(CAPTCHA.isEmpty()) {
            return false;
        }
        Session session = SecurityUtils.getSubject().getSession();
        CAPTCHA = CAPTCHA.toLowerCase();
        String v = (String) session.getAttribute(Constants.KAPTCHA_SESSION_KEY);
        return CAPTCHA.equals(v);
    }
}
