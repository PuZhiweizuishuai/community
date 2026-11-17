package com.buguagaoshu.community.component;

import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.util.AuthUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理员身份复核：requireAdmin 不能只看会话里有没有 admin 对象，
 * 用户被降权后旧会话必须失效。权限查询带 30 秒本地缓存，避免每个后台请求都查库。
 */
@Component
public class AdminPermissionChecker implements AuthUtil.AdminPermissionCheckerRef {

    private static final long CACHE_TTL_MILLIS = 30_000L;

    private record CachedPower(int power, long expireAt) {
    }

    private final UserPermissionService userPermissionService;
    private final Map<Long, CachedPower> cache = new ConcurrentHashMap<>();

    public AdminPermissionChecker(UserPermissionService userPermissionService) {
        this.userPermissionService = userPermissionService;
        AuthUtil.setAdminPermissionChecker(this);
    }

    @Override
    public boolean isAdmin(long userId) {
        long now = System.currentTimeMillis();
        CachedPower cached = cache.get(userId);
        if (cached != null && cached.expireAt() > now) {
            return cached.power() == 0;
        }
        UserPermission permission = userPermissionService.selectUserPermissionById(userId);
        int power = permission == null ? 1 : permission.getPower();
        cache.put(userId, new CachedPower(power, now + CACHE_TTL_MILLIS));
        return power == 0;
    }
}