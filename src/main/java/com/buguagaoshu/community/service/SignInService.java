package com.buguagaoshu.community.service;


import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:19
 * 登陆服务
 */
public interface SignInService {
    /**
     * 登陆方法
     * @param email 邮箱
     * @param password 密码
     * @param remember 记住我
     * @param captcha 验证码
     * @param request HttpServletRequest 负责检验验证码和获取用户登陆 IP
     * @return  返回登陆信息和 Token
     * */
    HashMap<String, Object> signIn(String email, String password, String remember, String captcha,HttpServletRequest request);
}
