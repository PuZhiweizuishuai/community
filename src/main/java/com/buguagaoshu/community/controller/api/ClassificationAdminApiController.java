package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.ClassificationMapper;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.model.Classification;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台「分类管理」：问题分类（提问/讨论/…）与话题所属分类（开发语言/…）的动态维护。
 *
 * <p>删除分类前会校验关联问题：只要该分类下还有问题，就拒绝删除并提示需要先迁移或删除问题，
 * 避免产生大量孤儿数据（这是原来枚举写死时无法做到的约束）。
 */
@RestController
@RequestMapping("/api/admin/classifications")
public class ClassificationAdminApiController {

    private final ClassificationMapper mapper;
    private final ClassificationCache cache;
    private final SiteCacheService siteCache;

    public ClassificationAdminApiController(ClassificationMapper mapper,
                                            ClassificationCache cache,
                                            SiteCacheService siteCache) {
        this.mapper = mapper;
        this.cache = cache;
        this.siteCache = siteCache;
    }

    @GetMapping
    public ResultDTO<Map<String, Object>> list(
            @RequestParam(value = "kind", required = false) String kind,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        List<Classification> all = mapper.getAll();
        List<Classification> result = (all == null ? List.<Classification>of() : all).stream()
                .filter(item -> StringUtils.isBlank(kind) || kind.equals(item.getKind()))
                .toList();
        result.forEach(item -> {
            if (Classification.KIND_QUESTION.equals(item.getKind())) {
                item.setUsageCount(mapper.countQuestionsByClassification(item.getName()));
            } else {
                item.setUsageCount(mapper.countQuestionsByTopicType(item.getCode()));
            }
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", result);
        data.put("total", result.size());
        return ResultDTO.okOf(data);
    }

    @PostMapping("/save")
    public ResultDTO<Classification> save(@RequestBody Classification body,
                                          HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        if (body == null || StringUtils.isBlank(body.getKind()) || StringUtils.isBlank(body.getName())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        String kind = body.getKind();
        if (!Classification.KIND_QUESTION.equals(kind) && !Classification.KIND_TOPIC.equals(kind)) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        String name = body.getName().trim();
        if (name.length() > 50) {
            return ResultDTO.errorOf(400, "分类名称不能超过 50 个字符");
        }
        long now = System.currentTimeMillis();
        boolean isNew = body.getId() == null || body.getId() <= 0;
        Classification old = isNew ? null : mapper.selectById(body.getId());
        if (!isNew && old == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }

        // 编辑时若前端未回传编号，保留原编号；新增时才自动分配下一个编号
        int code;
        if (body.getCode() == null || body.getCode() <= 0) {
            code = isNew ? mapper.maxCode(kind) + 1 : old.getCode();
        } else {
            code = body.getCode();
        }
        Classification sameName = mapper.selectByName(kind, name);
        Classification sameCode = mapper.selectByCode(kind, code);
        long selfId = old == null ? -1 : old.getId();
        if (sameName != null && sameName.getId() != selfId) {
            return ResultDTO.errorOf(409, "同一类型下已存在名称为「" + name + "」的分类");
        }
        if (sameCode != null && sameCode.getId() != selfId) {
            return ResultDTO.errorOf(409, "编号 " + code + " 已被其他分类占用");
        }

        body.setName(name);
        body.setCode(code);
        body.setKind(kind);
        body.setSortOrder(body.getSortOrder() == null ? code : body.getSortOrder());
        body.setStatus(body.getStatus() == null || body.getStatus() != 0 ? 1 : 0);
        body.setModifiedTime(now);

        if (isNew) {
            body.setCreateTime(now);
            mapper.insert(body);
        } else {
            body.setCreateTime(old.getCreateTime());
            mapper.update(body);
            // 历史数据同步：问题分类按名称存、话题分类按编号存
            if (Classification.KIND_QUESTION.equals(kind) && !old.getName().equals(name)) {
                mapper.renameQuestionClassification(old.getName(), name);
            }
            if (Classification.KIND_TOPIC.equals(kind)
                    && old.getCode() != null && old.getCode() != code) {
                mapper.renameTopicType(old.getCode(), code);
            }
        }
        cache.reload();
        siteCache.invalidate(SiteCacheService.CLASSIFICATION);
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META);
        return ResultDTO.okOf(body);
    }

    @PostMapping("/delete")
    public ResultDTO<Map<String, Object>> delete(@RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        long id = parseId(body.get("id"));
        Classification target = id <= 0 ? null : mapper.selectById(id);
        if (target == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }

        if (Classification.KIND_QUESTION.equals(target.getKind())) {
            long questions = mapper.countQuestionsByClassification(target.getName());
            if (questions > 0) {
                return ResultDTO.errorOf(409,
                        "「" + target.getName() + "」分类下还有 " + questions
                                + " 个问题，请先把这些问题移出该分类或删除后再删除分类");
            }
        } else {
            long questions = mapper.countQuestionsByTopicType(target.getCode());
            if (questions > 0) {
                return ResultDTO.errorOf(409,
                        "「" + target.getName() + "」分类下还有 " + questions
                                + " 个问题，请先把这些问题移出该话题或删除后再删除分类");
            }
            long topics = mapper.countTopicsByType(target.getCode());
            if (topics > 0) {
                return ResultDTO.errorOf(409,
                        "「" + target.getName() + "」分类下还有 " + topics
                                + " 个话题，请先把话题移动到其他分类或删除后再删除分类");
            }
        }

        mapper.deleteById(id);
        cache.reload();
        siteCache.invalidate(SiteCacheService.CLASSIFICATION);
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META);
        return ResultDTO.okOf(Map.of("deleted", true));
    }

    @PostMapping("/refresh")
    public ResultDTO<Map<String, Object>> refresh(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        cache.reload();
        siteCache.invalidate(SiteCacheService.CLASSIFICATION);
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META);
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
