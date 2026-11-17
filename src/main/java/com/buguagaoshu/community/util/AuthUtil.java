package com.buguagaoshu.community.util;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.BeanUtils;

/**
 * 前后端分离后的会话工具：所有需要登录态的 REST 接口统一从这里取用户。
 *
 * <p>会话仍然由 Spring Session + Redis 承载，前端通过 cookie（同源或 CORS 携带凭据）保持登录。
 */
public final class AuthUtil {

    public static final String SESSION_USER = "user";
    public static final String SESSION_ADMIN = "admin";

    /** 管理员权限复核器，由 AdminPermissionChecker 启动时注入 */
    private static volatile AdminPermissionCheckerRef adminPermissionChecker;

    /** 避免 util 包反向依赖 component 包的接口 */
    public interface AdminPermissionCheckerRef {
        boolean isAdmin(long userId);
    }

    public static void setAdminPermissionChecker(AdminPermissionCheckerRef checker) {
        adminPermissionChecker = checker;
    }

    private AuthUtil() {
    }

    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER);
        return value instanceof User user ? user : null;
    }

    public static User requireUser(HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null) {
            throw new CustomizeException(CustomizeErrorCode.NO_LOGIN);
        }
        return user;
    }

    public static User currentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_ADMIN);
        return value instanceof User user ? user : null;
    }

    public static User requireAdmin(HttpServletRequest request) {
        User admin = currentAdmin(request);
        if (admin == null) {
            throw new CustomizeException(CustomizeErrorCode.NO_ADMIN);
        }
        // 每次复核权限（带短 TTL 缓存）：被降权的旧管理员会话立即失效
        AdminPermissionCheckerRef checker = adminPermissionChecker;
        if (checker != null && !checker.isAdmin(admin.getId())) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(SESSION_ADMIN);
            }
            throw new CustomizeException(CustomizeErrorCode.NO_ADMIN);
        }
        return admin;
    }

    /**
     * 返回给前端的用户对象，剥离密码等敏感字段（不修改会话中的原对象）。
     */
    public static User safeUser(User source) {
        if (source == null) {
            return null;
        }
        User copy = new User();
        BeanUtils.copyProperties(source, copy);
        copy.setPassword(null);
        return copy;
    }
}
