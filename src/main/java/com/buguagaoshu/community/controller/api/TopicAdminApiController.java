package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.schedule.TagClassTasks;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台「话题分类」维护接口。
 *
 * <p>可以对 topic 表中的话题进行新增 / 编辑 / 删除 / 停用，并立即刷新
 * {@code TagClassCache}，避免改动要等十分钟定时同步才生效。
 */
@RestController
@RequestMapping("/api/admin/topics")
public class TopicAdminApiController {

    private final TagClassMapper tagClassMapper;
    private final TagClassTasks tagClassTasks;
    private final ClassificationCache classificationCache;
    private final SiteCacheService siteCache;

    public TopicAdminApiController(TagClassMapper tagClassMapper, TagClassTasks tagClassTasks,
                                   ClassificationCache classificationCache,
                                   SiteCacheService siteCache) {
        this.tagClassMapper = tagClassMapper;
        this.tagClassTasks = tagClassTasks;
        this.classificationCache = classificationCache;
        this.siteCache = siteCache;
    }

    /**
     * 话题列表，可按分类 type 与关键字过滤。
     */
    @GetMapping
    public ResultDTO<Map<String, Object>> list(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        List<TagClass> all = tagClassMapper.getAllTagClass();
        if (all == null) {
            all = List.of();
        }
        final String kw = StringUtils.isBlank(keyword) ? null : keyword.trim().toLowerCase();
        List<TagClass> result = all.stream()
                .filter(t -> type == null || type.isBlank() || String.valueOf(t.getType()).equals(type))
                .filter(t -> kw == null
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains(kw))
                        || (t.getSimpleDesc() != null && t.getSimpleDesc().toLowerCase().contains(kw)))
                .sorted(Comparator.comparingInt(TagClass::getType).thenComparingLong(TagClass::getId))
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("topics", result);
        data.put("total", result.size());
        return ResultDTO.okOf(data);
    }

    /**
     * 新增或编辑话题。id 为空/<=0 时为新增。
     */
    @PostMapping("/save")
    public ResultDTO<TagClass> save(@RequestBody TagClass body, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        if (body == null || StringUtils.isBlank(body.getTitle())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        String title = body.getTitle().trim();
        int type = body.getType();
        // 分类改为后台动态维护，编号必须存在于当前启用的话题分类中
        if (type < 1 || (!classificationCache.getTopicTypes().isEmpty()
                && !classificationCache.isValidTopicCode(type))) {
            return ResultDTO.errorOf(400, "所选话题分类不存在或已停用，请先在「分类管理」中配置");
        }
        long now = System.currentTimeMillis();
        body.setTitle(title);
        body.setType(type);
        body.setStatus(body.getStatus() == 0 ? 0 : 1);
        body.setModifiedTime(now);
        if (StringUtils.isBlank(body.getImage())) {
            body.setImage("/image/topicicon/default.svg");
        }

        TagClass sameTitle = tagClassMapper.getTagClassByTitle(title);
        boolean isNew = body.getId() <= 0;
        if (sameTitle != null && (isNew || sameTitle.getId() != body.getId())) {
            return ResultDTO.errorOf(409, "话题名称「" + title + "」已存在");
        }

        if (isNew) {
            body.setCreateTime(now);
            body.setTalkCount(0);
            body.setFollowCount(0);
            tagClassMapper.insertTagClass(body);
        } else {
            TagClass old = tagClassMapper.selectTagClassById(body.getId());
            if (old == null) {
                throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
            }
            body.setTalkCount(old.getTalkCount());
            body.setFollowCount(old.getFollowCount());
            body.setCreateTime(old.getCreateTime());
            tagClassMapper.updateTagClass(body);
        }
        tagClassTasks.TagClassCache();
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META + "tagClasses");
        return ResultDTO.okOf(body);
    }

    /**
     * 删除话题（物理删除）。已有帖子仍保留原标签文本，不会再命中话题缓存。
     */
    @PostMapping("/delete")
    public ResultDTO<Map<String, Object>> delete(@RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        long id = parseId(body.get("id"));
        if (id <= 0) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        int rows = tagClassMapper.deleteTagClassById(id);
        tagClassTasks.TagClassCache();
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META + "tagClasses");
        return ResultDTO.okOf(Map.of("deleted", rows > 0));
    }

    /** 立即把数据库里的话题同步到缓存 */
    @PostMapping("/refresh")
    public ResultDTO<Map<String, Object>> refresh(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        tagClassTasks.TagClassCache();
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META + "tagClasses");
        return ResultDTO.okOf(Map.of("refreshed", true));
    }

    private long parseId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return -1;
        }
    }
}
