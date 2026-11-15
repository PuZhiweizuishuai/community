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
