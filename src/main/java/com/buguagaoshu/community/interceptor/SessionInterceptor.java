package com.buguagaoshu.community.interceptor;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 前后端分离后只保留「记住我」token 恢复登录态的职责。
 *
 * <p>页面渲染已全部交给 Vue，原来的 notificationCount 注入 / ModelAndView 处理
 * 已移除，未读数改由 GET /api/auth/session 按需返回，避免每个 API 请求都查库。
 */
@Component
public class SessionInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final JwtUtil jwtUtil;

    public SessionInterceptor(UserService userService, OnlineUserService onlineUserService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            return true;
        }
        String token = readToken(request);
        if (token == null || token.isBlank()) {
            return true;
        }
        try {
            Claims claims = jwtUtil.parseJWT(token);
            User user = userService.selectUserById(Long.parseLong(claims.getId()));
            if (user != null) {
                user.clean();
                request.getSession().setAttribute("user", user);
            }
        } catch (Exception e) {
            onlineUserService.deleteOnlineUserByToken(token);
            HttpSession current = request.getSession(false);
            if (current != null) {
                current.removeAttribute("user");
            }
        }
        return true;
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
