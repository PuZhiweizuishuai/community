package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台缓存观测与手动刷新。
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequirePermission(PermissionCodes.ADMIN_SYSTEM)
public class CacheAdminApiController {

    private final SiteCacheService siteCache;

    public CacheAdminApiController(SiteCacheService siteCache) {
        this.siteCache = siteCache;
    }

    @GetMapping
    public ResultDTO<Map<String, Object>> stats(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(siteCache.stats());
    }

    @PostMapping("/refresh")
    public ResultDTO<Map<String, Object>> refresh(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        siteCache.invalidateAll();
        return ResultDTO.okOf(Map.of("refreshed", true));
    }

    /** 按前缀精确失效，例如 prefix=home:、prefix=meta: */
    @PostMapping("/invalidate")
    public ResultDTO<Map<String, Object>> invalidate(
            @RequestParam("prefix") String prefix, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        siteCache.invalidate(prefix);
        return ResultDTO.okOf(Map.of("invalidated", prefix));
    }
}
