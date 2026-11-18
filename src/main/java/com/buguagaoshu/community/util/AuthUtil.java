package com.buguagaoshu.community.util;

import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.BeanUtils;

import java.util.Set;

/**
 * 前后端分离后的会话工具：所有需要登录态的 REST 接口统一从这里取用户。
 *
 * <p>登录态只保留一份 {@code session.user}；管理员身份不再写入独立的 session 属性，
 * 而是由 RBAC 权限判定（{@code admin:access}）。这样后台与前台共用同一登录态，
 * 角色/权限变更后旧会话立即按新权限生效。
 */
public final class AuthUtil {

    public static final String SESSION_USER = "user";

    /** 权限查询器，由 PermissionChecker 启动时注入 */
    private static volatile PermissionCheckerRef permissionChecker;

    /** 避免 util 包反向依赖 component 包的接口 */
    public interface PermissionCheckerRef {
        Set<String> permissionsOf(long userId);

        boolean hasPermission(long userId, String code);
    }

    public static void setPermissionChecker(PermissionCheckerRef checker) {
        permissionChecker = checker;
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

    /**
     * 当前登录用户拥有的全部权限编码；未登录返回空集合。
     */
    public static Set<String> permissions(HttpServletRequest request) {
        return permissionsOf(currentUser(request));
    }

    public static Set<String> permissionsOf(User user) {
        PermissionCheckerRef checker = permissionChecker;
        if (user == null || checker == null) {
            return Set.of();
        }
        return checker.permissionsOf(user.getId());
    }

    public static boolean hasPermission(HttpServletRequest request, String code) {
        return hasPermission(currentUser(request), code);
    }

    public static boolean hasPermission(User user, String code) {
        PermissionCheckerRef checker = permissionChecker;
        return user != null && checker != null && checker.hasPermission(user.getId(), code);
    }

    /**
     * 以当前用户身份判断是否具备后台访问权限；不具备返回 null。
     */
    public static User currentAdmin(HttpServletRequest request) {
        User user = currentUser(request);
        return hasPermission(user, PermissionCodes.ADMIN_ACCESS) ? user : null;
    }

    /**
     * 要求当前用户具备后台访问权限。
     */
    public static User requireAdmin(HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null) {
            throw new CustomizeException(CustomizeErrorCode.NO_LOGIN);
        }
        if (!hasPermission(user, PermissionCodes.ADMIN_ACCESS)) {
            throw new CustomizeException(CustomizeErrorCode.NO_ADMIN);
        }
        return user;
    }

    /**
     * 要求当前用户具备指定权限点。
     */
    public static User requirePermission(HttpServletRequest request, String code) {
        User user = requireUser(request);
        if (!hasPermission(user, code)) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        return user;
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
