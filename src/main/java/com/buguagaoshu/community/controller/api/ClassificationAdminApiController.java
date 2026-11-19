package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.mapper.ClassificationMapper;
import com.buguagaoshu.community.service.ClassificationAdminService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.model.Classification;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
 * <p>保存/删除的事务与历史数据联动在 {@link ClassificationAdminService}；
 * 删除分类前会校验关联问题/话题，避免产生孤儿数据。
 */
@RestController
@RequestMapping("/api/admin/classifications")
@RequirePermission(PermissionCodes.ADMIN_CLASSIFICATION)
public class ClassificationAdminApiController {

    /** 只含 id 的强类型请求体，替代 Map 收参 */
    public record IdRequest(
            @NotNull(message = "分类 ID 不能为空")
            @Positive(message = "分类 ID 不合法") Long id) {
    }

    private final ClassificationMapper mapper;
    private final ClassificationCache cache;
    private final SiteCacheService siteCache;
    private final ClassificationAdminService classificationAdminService;

    public ClassificationAdminApiController(ClassificationMapper mapper,
                                            ClassificationCache cache,
                                            SiteCacheService siteCache,
                                            ClassificationAdminService classificationAdminService) {
        this.mapper = mapper;
        this.cache = cache;
        this.siteCache = siteCache;
        this.classificationAdminService = classificationAdminService;
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
        Map<Integer, Long> questionCounts = toCountMap(mapper.countQuestionsGroupByClassification());
        Map<Integer, Long> topicCounts = toCountMap(mapper.countQuestionsGroupByTopicType());
        result.forEach(item -> {
            Map<Integer, Long> counts = Classification.KIND_QUESTION.equals(item.getKind())
                    ? questionCounts : topicCounts;
            item.setUsageCount(counts.getOrDefault(item.getCode(), 0L));
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", result);
        data.put("total", result.size());
        return ResultDTO.okOf(data);
    }

    /** 聚合结果转 code → total 映射，忽略非法行 */
    private Map<Integer, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object code = row.get("code");
            Object total = row.get("total");
            if (code instanceof Number codeValue && total instanceof Number totalValue) {
                counts.put(codeValue.intValue(), totalValue.longValue());
            }
        }
        return counts;
    }

    @PostMapping("/save")
    public ResultDTO<Classification> save(@RequestBody Classification body,
                                          HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(classificationAdminService.save(body));
    }

    @PostMapping("/delete")
    public ResultDTO<Map<String, Object>> delete(@Valid @RequestBody IdRequest body,
                                                 HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        classificationAdminService.delete(body.id());
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
}