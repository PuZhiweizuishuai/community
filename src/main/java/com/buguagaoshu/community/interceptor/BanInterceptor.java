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
 */
@Component
public class BanInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/login",
            "/api/admin/login",
            "/api/auth/logout",
            "/api/admin/logout",
            "/api/register"
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
