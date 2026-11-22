package com.buguagaoshu.community.interceptor;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 基于 {@link RequirePermission} 的统一权限拦截器。
 *
 * <p>鉴权集中在拦截器，新增接口时只需标注权限点，避免遗漏。
 * 方法级注解优先于类级注解。
 *
 * <p><b>后台路径默认拒绝</b>：{@code /api/admin/**} 下未标注 {@code @RequirePermission}
 * 的接口一律要求 {@code admin:access}，只有明确列入 {@link #ANONYMOUS_ADMIN_PATHS}
 * 的入口（登录、退出、路由守卫用的会话探测）才允许匿名访问。
 * 这样可以防止新增后台接口时忘记标注注解导致未授权访问。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    /** 后台路径下允许匿名访问的入口：登录/退出/前端路由守卫的会话探测 */
    private static final Set<String> ANONYMOUS_ADMIN_PATHS = Set.of(
            "/api/admin/login",
            "/api/admin/logout",
            "/api/admin/session");

    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission required = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequirePermission.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequirePermission.class);
        }
        if (required == null) {
            requireAdminAccessForAdminPath(request);
            return true;
        }
        User user = AuthUtil.currentUser(request);
        if (user == null) {
            throw new CustomizeException(CustomizeErrorCode.NO_LOGIN);
        }
        String code = required.value();
        // admin:* 权限以「后台访问」为前置门槛；content:* / file:* 是普通用户权限，不要求后台身份
        if (code.startsWith("admin:") && !AuthUtil.hasPermission(user, PermissionCodes.ADMIN_ACCESS)) {
            throw new CustomizeException(CustomizeErrorCode.NO_ADMIN);
        }
        if (!AuthUtil.hasPermission(user, code)) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        return true;
    }

    /** 后台路径兜底：未标注权限点时默认按「需要 admin:access」处理 */
    private void requireAdminAccessForAdminPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith(ADMIN_PATH_PREFIX) || ANONYMOUS_ADMIN_PATHS.contains(path)) {
            return;
        }
        User user = AuthUtil.currentUser(request);
        if (user == null) {
            throw new CustomizeException(CustomizeErrorCode.NO_LOGIN);
        }
        if (!AuthUtil.hasPermission(user, PermissionCodes.ADMIN_ACCESS)) {
            throw new CustomizeException(CustomizeErrorCode.NO_ADMIN);
        }
    }
}
