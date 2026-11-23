package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.LoginRequest;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserLoginHistory;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SignInService;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.service.UserLoginHistoryService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录态相关 REST 接口。
 *
 * <p>统一以 Spring Session（SESSION cookie）为准；token cookie 仅用于「记住我」
 * 登录态恢复（HttpOnly，JS 不可读，也不在响应体回传）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final SignInService signInService;
    private final NotificationService notificationService;
    private final OnlineUserService onlineUserService;
    private final UserBanService userBanService;
    private final UserLoginHistoryService userLoginHistoryService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthApiController(SignInService signInService,
                             NotificationService notificationService,
                             OnlineUserService onlineUserService,
                             UserBanService userBanService,
                             UserLoginHistoryService userLoginHistoryService) {
        this.signInService = signInService;
        this.notificationService = notificationService;
        this.onlineUserService = onlineUserService;
        this.userBanService = userBanService;
        this.userLoginHistoryService = userLoginHistoryService;
    }

    /**
     * 当前登录用户（含未读消息数、后台权限）。前端启动时调用一次。
     *
     * <p>是否管理员由 RBAC 权限决定，因此同时返回 permissions，供前端路由守卫与菜单渲染。
     */
    @GetMapping("/session")
    public ResultDTO<Map<String, Object>> session(HttpServletRequest request) {
        User user = AuthUtil.currentUser(request);
        User admin = AuthUtil.currentAdmin(request);
        Map<String, Object> ban = user == null ? null : userBanService.banInfo(user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", AuthUtil.safeUser(user));
        data.put("admin", AuthUtil.safeUser(admin));
        data.put("permissions", AuthUtil.permissionsOf(user));
        data.put("banned", ban != null && Boolean.TRUE.equals(ban.get("banned")));
        data.put("ban", ban);
        data.put("notificationCount", user == null ? 0L
                : notificationService.getAllNotificationNoReadNumber(user.getId()));
        data.put("loggedIn", user != null);
        data.put("isAdmin", admin != null);
        return ResultDTO.okOf(data);
    }

    /** 当前用户的登录历史，用于自查异常登录 */
    @GetMapping("/login-history")
    public ResultDTO<PaginationDto<UserLoginHistory>> loginHistory(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "10") String size,
            HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        return ResultDTO.okOf(userLoginHistoryService.pageByUser(user.getId(), page, size));
    }

    /**
     * 登录。成功后轮换会话 id（防会话固定），并设置 HttpOnly 的 token cookie。
     */
    @PostMapping("/login")
    public ResultDTO<Map<String, Object>> login(@Valid @RequestBody LoginRequest body,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        try {
            HashMap<String, Object> signInMsg = signInService.signIn(
                    body.getEmail(), body.getPassword(), body.getRememberValue(),
                    body.getCaptchaValue(), request);
            User user = (User) signInMsg.get("user");
            String token = (String) signInMsg.get("token");
            // 登录前可能已有匿名会话（验证码），轮换 id 防止会话固定攻击
            request.getSession();
            request.changeSessionId();
            request.getSession().setAttribute(AuthUtil.SESSION_USER, user);

            response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie(token, (Integer) signInMsg.get("time")).toString());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", AuthUtil.safeUser(user));
            return ResultDTO.okOf(data);
        } catch (CustomizeException e) {
            return ResultDTO.errorOf(e);
        }
    }

    /**
     * 退出登录：吊销整个会话（前台 user 与后台 admin 一并失效）、在线记录与 token cookie。
     */
    @PostMapping("/logout")
    public ResultDTO<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        User user = AuthUtil.currentUser(request);
        if (user != null) {
            onlineUserService.deleteOnlineUserById(user.getId());
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie("", 0).toString());
        return ResultDTO.okOf(Map.of("loggedOut", true));
    }

    private ResponseCookie tokenCookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from("token", value)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
    }
}