package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.LoginRequest;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SignInService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录态相关 REST 接口。
 *
 * <p>统一以 Spring Session（SESSION cookie）为准；token cookie 仅用于兼容旧版「记住我」
 * 与 SessionInterceptor 的登录态恢复。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final SignInService signInService;
    private final NotificationService notificationService;
    private final OnlineUserService onlineUserService;

    public AuthApiController(SignInService signInService,
                             NotificationService notificationService,
                             OnlineUserService onlineUserService) {
        this.signInService = signInService;
        this.notificationService = notificationService;
        this.onlineUserService = onlineUserService;
    }

    /**
     * 当前登录用户（含未读消息数、后台管理员态）。前端启动时调用一次。
     */
    @GetMapping("/session")
    public ResultDTO<Map<String, Object>> session(HttpServletRequest request) {
        User user = AuthUtil.currentUser(request);
        User admin = AuthUtil.currentAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", AuthUtil.safeUser(user));
        data.put("admin", AuthUtil.safeUser(admin));
        data.put("notificationCount", user == null ? 0L
                : notificationService.getAllNotificationNoReadNumber(user.getId()));
        data.put("loggedIn", user != null);
        data.put("isAdmin", admin != null);
        data.put("token", readToken(request));
        return ResultDTO.okOf(data);
    }

    /**
     * 登录。成功后把用户写入会话，并设置可持久化的 token cookie。
     */
    @PostMapping("/login")
    public ResultDTO<Map<String, Object>> login(@RequestBody LoginRequest body,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        try {
            HashMap<String, Object> signInMsg = signInService.signIn(
                    body.getEmail(), body.getPassword(), body.getRememberValue(),
                    body.getCaptchaValue(), request);
            User user = (User) signInMsg.get("user");
            String token = (String) signInMsg.get("token");
            request.getSession().setAttribute(AuthUtil.SESSION_USER, user);

            Cookie cookie = new Cookie("token", token);
            cookie.setPath("/");
            cookie.setMaxAge((Integer) signInMsg.get("time"));
            cookie.setHttpOnly(false);
            response.addCookie(cookie);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", AuthUtil.safeUser(user));
            data.put("token", token);
            return ResultDTO.okOf(data);
        } catch (CustomizeException e) {
            return ResultDTO.errorOf(e);
        }
    }

    /**
     * 退出登录：清理前台会话、在线记录与 token cookie。
     */
    @PostMapping("/logout")
    public ResultDTO<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        User user = AuthUtil.currentUser(request);
        if (user != null) {
            onlineUserService.deleteOnlineUserById(user.getId());
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(AuthUtil.SESSION_USER);
        }
        Cookie cookie = new Cookie("token", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResultDTO.okOf(Map.of("loggedOut", true));
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
