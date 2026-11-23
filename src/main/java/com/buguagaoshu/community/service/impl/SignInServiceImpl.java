package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserLoginHistory;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SignInService;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.service.UserLoginHistoryService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.JwtUtil;
import com.buguagaoshu.community.util.StringUtil;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SignInServiceImpl implements SignInService {

    /** 「记住我」token 有效期：7 天 */
    private static final long REMEMBER_ME_TTL_MILLIS = 7 * 24 * 3600 * 1000L;

    /** 单 IP 失败阈值：比单账号阈值宽，避免 NAT 出口下正常用户互相影响 */
    private static final int IP_MAX_FAILURES = 30;

    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final OnlineUserService onlineUserService;

    private final JwtUtil jwtUtil;

    private final RateLimitService rateLimitService;

    private final UserBanService userBanService;

    private final UserLoginHistoryService userLoginHistoryService;

    /** 非「记住我」token 有效期，默认 2 小时 */
    @Value("${jwt.ttl}")
    private long defaultTtlMillis;

    @Autowired
    public SignInServiceImpl(UserPermissionService userPermissionService, UserService userService,
                             OnlineUserService onlineUserService, JwtUtil jwtUtil,
                             RateLimitService rateLimitService, UserBanService userBanService,
                             UserLoginHistoryService userLoginHistoryService) {
        this.userPermissionService = userPermissionService;
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.jwtUtil = jwtUtil;
        this.rateLimitService = rateLimitService;
        this.userBanService = userBanService;
        this.userLoginHistoryService = userLoginHistoryService;
    }

    @Override
    public HashMap<String, Object> signIn(String email, String password, String remember, String captcha, HttpServletRequest request) {
        if (!CaptchaUtil.ver(captcha, request)) {
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        if (StringUtil.isEmpty(email) || StringUtil.isEmpty(password)) {
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_EMAIL_OR_PASSWORD_NULL);
        }
        // 双维度限流：邮箱维度防单账号爆破，IP 维度防「大量账号 + 同一弱口令」撞库
        String emailLimitKey = "login:" + email.toLowerCase();
        String ip = IpUtil.getIpAddr(request);
        String ipLimitKey = "login:ip:" + ip;
        if (rateLimitService.isLocked(emailLimitKey) || rateLimitService.isLocked(ipLimitKey, IP_MAX_FAILURES)) {
            throw new CustomizeException(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        boolean rememberMe = remember != null;
        User user = userService.selectUserByEmail(email);
        // 账号不存在与密码错误返回同一错误码，避免攻击者枚举已注册邮箱
        if (user == null || !StringUtil.judgePassword(password, user.getPassword())) {
            rateLimitService.recordFailure(emailLimitKey);
            rateLimitService.recordFailure(ipLimitKey);
            log.warn("登录失败（邮箱或密码错误），邮箱 {}，IP {}", StringUtil.maskEmail(email), ip);
            throw new CustomizeException(CustomizeErrorCode.SIGN_IN_CREDENTIALS_ERROR);
        }
        // 登录成功只清账号维度计数；IP 维度计数保留到窗口自然过期，
        // 否则攻击者用一个自己的正常账号即可清空 IP 计数
        rateLimitService.reset(emailLimitKey);

        if (userBanService.isBanned(user.getId())) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(),
                    userBanService.bannedMessage(user.getId()));
        }

        long ttlMillis = rememberMe ? REMEMBER_ME_TTL_MILLIS : defaultTtlMillis;
        UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
        // token 只携带用户 ID，其余信息由服务端按需查库，避免敏感字段在 payload 中明文可见
        String token = jwtUtil.createJWT(user.getId(), ttlMillis);
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
        userLoginHistoryService.record(user.getId(), ip, request.getHeader("User-Agent"),
                UserLoginHistory.TYPE_WEB);
        return hashMap;
    }
}