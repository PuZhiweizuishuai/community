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

/**
 * 基于 {@link RequirePermission} 的统一权限拦截器。
 *
 * <p>不再在每个后台接口里手写 requireAdmin：鉴权集中在拦截器，
 * 新增接口时只要标注权限点即可，避免遗漏。方法级注解优先于类级注解。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

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
}
