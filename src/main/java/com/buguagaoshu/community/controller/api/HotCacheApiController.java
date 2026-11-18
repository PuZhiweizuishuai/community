package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.schedule.HotQuestionTasks;
import com.buguagaoshu.community.schedule.HotTagTasks;
import com.buguagaoshu.community.schedule.HotUserTasks;
import com.buguagaoshu.community.schedule.TagClassTasks;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.AuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 手动刷新缓存：写操作一律 POST（GET 写操作在 SameSite=Lax 下存在 CSRF 面）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-19 20:49
 */
@RestController
@Slf4j
@RequirePermission(PermissionCodes.ADMIN_SYSTEM)
public class HotCacheApiController {
    private final HotTagTasks hotTagTasks;

    private final HotQuestionTasks hotQuestionTasks;

    private final HotUserTasks hotUserTasks;

    private final TagClassTasks tagClassTasks;
    private final SiteCacheService siteCache;

    @Autowired
    public HotCacheApiController(HotTagTasks hotTagTasks, HotQuestionTasks hotQuestionTasks,
                                 HotUserTasks hotUserTasks, TagClassTasks tagClassTasks,
                                 SiteCacheService siteCache) {
        this.hotTagTasks = hotTagTasks;
        this.hotQuestionTasks = hotQuestionTasks;
        this.hotUserTasks = hotUserTasks;
        this.tagClassTasks = tagClassTasks;
        this.siteCache = siteCache;
    }

    @PostMapping("/api/admin/updateHotTag")
    public ResultDTO<Object> updateHatTag(HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        log.info("管理员 {} 开始手动更新热门话题", admin.getId());
        hotTagTasks.hotTagsCurrentTime();
        siteCache.invalidate(SiteCacheService.META + "hots");
        log.info("热门话题手动更新完成！");
        return ResultDTO.okOf();
    }

    @PostMapping("/api/admin/updateHotQuestion")
    public ResultDTO<Object> updateHotQuestion(HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        log.info("管理员 {} 开始手动更新热门问题", admin.getId());
        hotQuestionTasks.hotQuestionCurrentTime();
        siteCache.invalidate(SiteCacheService.META + "hotQuestions");
        log.info("热门问题手动更新完成！");
        return ResultDTO.okOf();
    }

    @PostMapping("/api/admin/updateHotUser")
    public ResultDTO<Object> updateHotUser(HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        log.info("管理员 {} 开始手动更新活跃用户", admin.getId());
        hotUserTasks.hotUserCurrentTime();
        siteCache.invalidate(SiteCacheService.META + "hotUsers");
        log.info("活跃用户手动更新完成！");
        return ResultDTO.okOf();
    }

    @PostMapping("/api/admin/updateTagClass")
    public ResultDTO<Object> updateTagClass(HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        log.info("管理员 {} 开始手动同步话题页", admin.getId());
        tagClassTasks.TagClassCache();
        siteCache.invalidate(SiteCacheService.TOPICS);
        log.info("话题同步完成！");
        return ResultDTO.okOf();
    }
}