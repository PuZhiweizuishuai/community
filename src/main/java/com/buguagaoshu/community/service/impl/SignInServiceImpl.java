package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SignInService;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.JwtUtil;
import com.buguagaoshu.community.util.StringUtil;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 16:20
 * 登陆方法的具体实现
 */
@Service
public class SignInServiceImpl implements SignInService {

    /** 「记住我」token 有效期：7 天 */
    private static final long REMEMBER_ME_TTL_MILLIS = 7 * 24 * 3600 * 1000L;

    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final OnlineUserService onlineUserService;

    private final JwtUtil jwtUtil;

    private final RateLimitService rateLimitService;

    private final UserBanService userBanService;

    /** 非「记住我」token 有效期，默认 2 小时 */
    @Value("${jwt.ttl}")
    private long defaultTtlMillis;

    @Autowired
    public SignInServiceImpl(UserPermissionService userPermissionService, UserService userService,
                             OnlineUserService onlineUserService, JwtUtil jwtUtil,
                             RateLimitService rateLimitService, UserBanService userBanService) {
        this.userPermissionService = userPermissionService;
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.jwtUtil = jwtUtil;
        this.rateLimitService = rateLimitService;
        this.userBanService = userBanService;
    }

    @Override
    public HashMap<String, Object> signIn(String email, String password, String remember, String captcha, HttpServletRequest request) {
        if (!CaptchaUtil.ver(captcha, request)) {
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        if (StringUtil.isEmpty(email) || StringUtil.isEmpty(password)) {
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_EMAIL_OR_PASSWORD_NULL);
        }
        String limitKey = "login:" + email.toLowerCase();
        if (rateLimitService.isLocked(limitKey)) {
            throw new CustomizeException(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        boolean rememberMe = remember != null;
        User user = userService.selectUserByEmail(email);
        if (user == null) {
            rateLimitService.recordFailure(limitKey);
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_EMAIL_ERROR);
        }
        if (!StringUtil.judgePassword(password, user.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_PASSWORD_ERROR);
        }
        rateLimitService.reset(limitKey);

        if (userBanService.isBanned(user.getId())) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(),
                    userBanService.bannedMessage(user.getId()));
        }

        long ttlMillis = rememberMe ? REMEMBER_ME_TTL_MILLIS : defaultTtlMillis;
        UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
        String token = jwtUtil.createJWT(user.getId(), user.getUserId(), user.getUserName(), email,
                userPermission.getPower(), ttlMillis);
        long now = System.currentTimeMillis();
        onlineUserService.insertOnlineUser(new OnlineUser(user.getId(), user.getUserName(), token,
                IpUtil.getIpAddr(request), now, now + ttlMillis));
        user.setPower(userPermission.getPower());
        HashMap<String, Object> hashMap = new HashMap<>(4);
        hashMap.put("success", true);
        hashMap.put("token", token);
        user.clean();
        hashMap.put("user", user);
        // cookie MaxAge（秒）：非记住我为会话级 cookie
        hashMap.put("time", rememberMe ? (int) (ttlMillis / 1000) : -1);
        userService.updateUserLastTimeById(user.getId(), now);
        return hashMap;
    }
}