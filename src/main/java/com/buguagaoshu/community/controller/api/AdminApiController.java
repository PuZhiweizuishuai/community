package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.AdminLoginRequest;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserLoginHistory;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.service.UserLoginHistoryService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.buguagaoshu.community.model.table.UserTableDef.USER;

/**
 * 后台登录 / 退出。
 *
 * <p>管理员不再使用独立的 session 身份：这里与前台登录共用 {@code session.user}，
 * 登录成功后按 RBAC 校验 {@code admin:access} 权限。是否管理员完全由角色权限决定，
 * 因此角色调整后无需重新登录即可生效。
 */
@RestController
@Slf4j
public class AdminApiController {

    /** 单 IP 失败阈值：与前台登录共用同一口径 */
    private static final int IP_MAX_FAILURES = 30;

    private final UserMapper userMapper;
    private final OnlineUserService onlineUserService;
    private final UserPermissionService userPermissionService;
    private final UserBanService userBanService;
    private final RateLimitService rateLimitService;
    private final UserLoginHistoryService userLoginHistoryService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public AdminApiController(UserMapper userMapper, OnlineUserService onlineUserService,
                              UserPermissionService userPermissionService,
                              UserBanService userBanService,
                              RateLimitService rateLimitService,
                              UserLoginHistoryService userLoginHistoryService) {
        this.userMapper = userMapper;
        this.onlineUserService = onlineUserService;
        this.userPermissionService = userPermissionService;
        this.userBanService = userBanService;
        this.rateLimitService = rateLimitService;
        this.userLoginHistoryService = userLoginHistoryService;
    }

    @PostMapping({"/api/admin/login", "/admin/login"})
    public ResultDTO<Map<String, Object>> adminLogin(@Valid AdminLoginRequest body,
                                                      HttpServletRequest request, HttpServletResponse response) {
        if (!CaptchaUtil.ver(body.getValidateCode(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        // 与前台登录一致：邮箱 + IP 双维度限流，且失败提示不区分账号/密码错误
        String limitKey = "adminLogin:" + body.getEmail().toLowerCase();
        String ip = IpUtil.getIpAddr(request);
        String ipLimitKey = "adminLogin:ip:" + ip;
        if (rateLimitService.isLocked(limitKey) || rateLimitService.isLocked(ipLimitKey, IP_MAX_FAILURES)) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where(USER.EMAIL.eq(body.getEmail())));
        if (user == null || !StringUtil.judgePassword(body.getPassword(), user.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            rateLimitService.recordFailure(ipLimitKey);
            log.warn("后台登录失败（邮箱或密码错误），邮箱 {}，IP {}", StringUtil.maskEmail(body.getEmail()), ip);
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CREDENTIALS_ERROR);
        }
        if (userBanService.isBanned(user.getId())) {
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.FORBIDDEN.getCode(),
                    userBanService.bannedMessage(user.getId()));
        }
        if (!AuthUtil.hasPermission(user, PermissionCodes.ADMIN_ACCESS)) {
            log.info("用户 {} 尝试登录后台，但未授予后台访问权限", user.getId());
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.NO_ADMIN);
        }
        rateLimitService.reset(limitKey);
        UserPermission permission = userPermissionService.selectUserPermissionById(user.getId());
        user.setPower(permission == null ? 1 : permission.getPower());
        user.clean();
        // 切换账号时清理旧的「记住我」token，避免会话过期后恢复成上一个账号
        String previousToken = readToken(request);
        if (previousToken != null) {
            onlineUserService.deleteOnlineUserByToken(previousToken);
            response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie("", 0).toString());
        }
        request.getSession();
        request.changeSessionId();
        request.getSession().setAttribute(AuthUtil.SESSION_USER, user);
        long now = System.currentTimeMillis();
        userMapper.updateUserLastTimeById(user.getId(), now);
        userLoginHistoryService.record(user.getId(), ip, request.getHeader("User-Agent"),
                UserLoginHistory.TYPE_ADMIN);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", AuthUtil.safeUser(user));
        data.put("permissions", AuthUtil.permissionsOf(user));
        return ResultDTO.okOf(data);
    }

    @PostMapping({"/api/admin/logout", "/admin/signOut"})
    public ResultDTO<Object> adminLogout(HttpServletRequest request, HttpServletResponse response) {
        User user = AuthUtil.currentUser(request);
        if (user != null) {
            onlineUserService.deleteOnlineUserById(user.getId());
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.addHeader(HttpHeaders.SET_COOKIE, tokenCookie("", 0).toString());
        return ResultDTO.okOf();
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
