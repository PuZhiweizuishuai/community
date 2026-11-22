package com.buguagaoshu.community.interceptor;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 封禁拦截器：被封禁用户仍可浏览与退出登录，但所有写操作统一拒绝。
 *
 * <p>登录接口放行，保证被误封或换账号时可以重新登录；真正的登录封禁在
 * SignInService / 后台登录中单独校验。
 *
 * <p>注册接口<b>不放行</b>：被封禁用户带着会话去注册新账号会被拦下，
 * 避免「换个邮箱重新注册」绕过封禁。匿名注册不受影响——
 * {@link AuthUtil#currentUser} 对匿名请求返回 null，条件不成立。
 */
@Component
public class BanInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/login",
            "/api/admin/login",
            "/api/auth/logout",
            "/api/admin/logout"
    );

    private final UserBanService userBanService;

    public BanInterceptor(UserBanService userBanService) {
        this.userBanService = userBanService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String path = request.getRequestURI();
        if (ALLOWED_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        User user = AuthUtil.currentUser(request);
        if (user != null && userBanService.isBanned(user.getId())) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(),
                    userBanService.bannedMessage(user.getId()));
        }
        return true;
    }
}
