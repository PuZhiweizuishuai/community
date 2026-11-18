package com.buguagaoshu.community.component;

import com.buguagaoshu.community.mapper.SysPermissionMapper;
import com.buguagaoshu.community.util.AuthUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户权限查询与短 TTL 缓存。
 *
 * <p>后台权限按 RBAC 实时计算，但每个请求都查三次表成本过高，因此做 30 秒本地缓存：
 * 角色 / 权限变更时由 {@code RbacService} 主动 evict，正常请求直接命中缓存。
 * 多实例部署时其它节点最迟 30 秒感知变更。
 */
@Component
public class PermissionChecker implements AuthUtil.PermissionCheckerRef {

    private static final long CACHE_TTL_MILLIS = 30_000L;

    private record CachedPermissions(Set<String> codes, long expireAt) {
    }

    private final SysPermissionMapper sysPermissionMapper;
    private final Map<Long, CachedPermissions> cache = new ConcurrentHashMap<>();

    public PermissionChecker(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
        AuthUtil.setPermissionChecker(this);
    }

    @Override
    public Set<String> permissionsOf(long userId) {
        long now = System.currentTimeMillis();
        CachedPermissions cached = cache.get(userId);
        if (cached != null && cached.expireAt() > now) {
            return cached.codes();
        }
        Set<String> codes = Set.copyOf(sysPermissionMapper.selectCodesByUserId(userId));
        cache.put(userId, new CachedPermissions(codes, now + CACHE_TTL_MILLIS));
        return codes;
    }

    @Override
    public boolean hasPermission(long userId, String code) {
        return code != null && permissionsOf(userId).contains(code);
    }

    /** 单个用户角色变更后失效 */
    public void evict(long userId) {
        cache.remove(userId);
    }

    /** 角色或角色权限变更后失效全部用户 */
    public void evictAll() {
        cache.clear();
    }
}
