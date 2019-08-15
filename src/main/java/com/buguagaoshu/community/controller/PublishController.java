package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String getPublishPage() {
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
                              @RequestParam("CAPTCHA") String CAPTCHA,
                              HttpServletRequest request,
                              Model model) {
        Question question = new Question();
        question.setTitle(title);

        question.setClassification(classification);
        question.setDescription(description);
        question.setTag(tag);
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        question.setUserId(user.getId());
        question.setCreateTime(StringUtil.getNowTime());
        question.setAlterTime(StringUtil.getNowTime());


        if(!check(question, model)) {
            return StringUtil.jumpWebLangeParameter("publish", false, request);
        }
        questionService.createQuestion(question);


        System.out.println(user.getId());
        System.out.println(title);
        System.out.println(classification);
        System.out.println(description);
        //System.out.println(file);
        System.out.println(tag);
        return StringUtil.jumpWebLangeParameter("/", true, request);
    }


    private boolean check(Question question, Model model) {
        if(!question.getTitle().equals("") && !question.getClassification().equals("")
                && !question.getDescription().equals("") && !question.getClassification().equals("null")) {
            return true;
        }
        if(question.getTitle().equals("") || question.getTitle() == null) {
            model.addAttribute("titleMessage", "标题不能为空！");
        }
        if(question.getClassification().equals("") || question.getClassification().equals("null") || question.getClassification() == null) {
            model.addAttribute("classMessage", "分类不能为空！");
        }
        if(question.getDescription().equals("") || question.getDescription() != null) {
            model.addAttribute("textMessage", "内容不能为空！");
        }
        model.addAttribute("cacheTitleMsg", question.getTitle());
        model.addAttribute("cacheTextMsg", question.getDescription());
        return false;
    }
}
