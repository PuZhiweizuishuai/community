package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.User;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

/**
 * 注册控制
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 16:29
 */
@RestController
public class SignUpController {
    @GetMapping("/sign-up")
    public ModelAndView signUp() {
        ModelAndView signUpView = new ModelAndView();
        signUpView.setViewName("SignUp");
        return signUpView;
    }


    /**
     * 注册新用户
     * */
    @PostMapping("/api/register")
    @ResponseBody
    public HashMap<String, Object> userSignUp(@Valid User user, Errors errors) {
        System.out.println( StringUtil.getAge(user.getBirthday()));
        // 校验数据并返回错误信息
        HashMap<String, Object> infoMap = new HashMap<>(2);
        List<ObjectError> oes = errors.getAllErrors();
        if(user.getUserName().isEmpty()) {
            infoMap.put("userName","用户名不能为空！");
        }
        if(user.getEmail().isEmpty()) {
            infoMap.put("email","email不能为空！");
        }
        if(oes.size() != 0) {
            for(ObjectError oe : oes) {
                String key = null;
                String msg = null;
                if(oe instanceof FieldError) {
                    FieldError fe = (FieldError) oe;
                    key = fe.getField();
                } else {
                    key = oe.getObjectName();
                }
                msg = oe.getDefaultMessage();
                infoMap.put(key, msg);
            }
            return infoMap;
        }
        int age = StringUtil.getAge(user.getBirthday());
        if(age == -1) {
            infoMap.put("birthday","生日输入超出当前日期！");
        }
        if(infoMap.size() != 0) {
            return infoMap;
        }
        // 补全数据
        setDefaultHeadUrl(user);

        // 插入数据
        return StringUtil.dealResultMessage(false, "暂时测试成功！");
    }

    /**
     * 补全默认头像信息
     * */
    private void setDefaultHeadUrl(User user) {
        if(user.getSex().equals("男")) {
            user.setHeadUrl("image/head/boyhead.png");
        } else if(user.getSex().equals("女")) {
            user.setHeadUrl("image/head/girlhead.png");
        } else {
            user.setHeadUrl("image/head/nohead.png");
        }
    }
}
