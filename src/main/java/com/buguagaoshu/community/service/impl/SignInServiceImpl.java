package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SignInService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.JwtUtil;
import com.buguagaoshu.community.util.StringUtil;
import com.wf.captcha.utils.CaptchaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:20
 * 登陆方法的具体实现
 */
@Service
public class SignInServiceImpl implements SignInService {
    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final OnlineUserService onlineUserService;

    private final JwtUtil jwtUtil;

    @Autowired
    public SignInServiceImpl(UserPermissionService userPermissionService, UserService userService,
                             OnlineUserService onlineUserService, JwtUtil jwtUtil) {
        this.userPermissionService = userPermissionService;
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public HashMap<String, Object> signIn(String email, String password, String remember, String captcha, HttpServletRequest request) {
        if (!CaptchaUtil.ver(captcha, request)) {
            // 清除失效的验证码
            CaptchaUtil.clear(request);
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        CaptchaUtil.clear(request);
        if (StringUtil.isEmpty(email) || StringUtil.isEmpty(password)) {
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_EMAIL_OR_PASSWORD_NULL);
        }
        boolean rememberMe = false;
        if (remember != null) {
            rememberMe = true;
        }
        User user = userService.selectUserByEmail(email);
        if (user != null) {
            if (StringUtil.judgePassword(password, user.getPassword())) {
                int ttl = -1;
                UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
                // 如果选择记住密码，则持久化 cookie
                // 否则设置关闭浏览器直接清除 cookie
                if (rememberMe) {
                    // token 有效期为七天
                    ttl = 604800;
                    jwtUtil.setTtl(604800000);
                }
                String token = jwtUtil.createJWT(user.getId(), user.getUserId(), user.getUserName(), email, userPermission.getPower());
                onlineUserService.insertOnlineUser(new OnlineUser(user.getId(), user.getUserName(), token, IpUtil.getIpAddr(request), System.currentTimeMillis(), System.currentTimeMillis()+jwtUtil.getTtl()));
                user.setPower(userPermission.getPower());
                HashMap<String, Object> hashMap = new HashMap<>(3);
                hashMap.put("success", true);
                hashMap.put("token", token);
                user.clean();
                hashMap.put("user", user);
                hashMap.put("time", ttl);
                // 跟新登陆时间
                userService.updateUserLastTimeById(user.getId(), StringUtil.getNowTime());
                return hashMap;
            } else {
                throw new CustomizeException(CustomizeErrorCode.SIGN_IN_PASSWORD_ERROR);
            }
        } else {
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_EMAIL_ERROR);
        }
    }
}
