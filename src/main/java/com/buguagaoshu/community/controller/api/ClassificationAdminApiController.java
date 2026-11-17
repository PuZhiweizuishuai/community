package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.ClassificationMapper;
import com.buguagaoshu.community.service.ClassificationAdminService;
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
 * <p>保存/删除的事务与历史数据联动在 {@link ClassificationAdminService}；
 * 删除分类前会校验关联问题/话题，避免产生孤儿数据。
 */
@RestController
@RequestMapping("/api/admin/classifications")
public class ClassificationAdminApiController {

    /** 只含 id 的强类型请求体，替代 Map 收参 */
    public record IdRequest(Long id) {
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
        return ResultDTO.okOf(classificationAdminService.save(body));
    }

    @PostMapping("/delete")
    public ResultDTO<Map<String, Object>> delete(@RequestBody IdRequest body,
                                                 HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        if (body == null || body.id() == null || body.id() <= 0) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
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