package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.cache.HotQuestionCache;
import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.cache.HotUserCache;
import com.buguagaoshu.community.cache.TagCache;
import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.enums.QuestionClassType;
import com.buguagaoshu.community.enums.QuestionSortType;
import com.buguagaoshu.community.enums.TagClassTypeEnum;
import com.buguagaoshu.community.model.Classification;
import com.buguagaoshu.community.service.AdvertisementService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.SiteCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全站公共数据：导航、侧边栏、下拉选项、广告位等。
 * 前端启动时拉取一次，避免每个页面接口重复携带这些内容。
 */
@RestController
@RequestMapping("/api/meta")
public class MetaApiController {

    private final HotTagCache hotTagCache;
    private final HotQuestionCache hotQuestionCache;
    private final HotUserCache hotUserCache;
    private final AdvertisementCache advertisementCache;
    private final TagClassCache tagClassCache;
    private final AdvertisementService advertisementService;
    private final OnlineUserService onlineUserService;
    private final ClassificationCache classificationCache;
    private final SiteCacheService siteCache;

    public MetaApiController(HotTagCache hotTagCache,
                             HotQuestionCache hotQuestionCache,
                             HotUserCache hotUserCache,
                             AdvertisementCache advertisementCache,
                             TagClassCache tagClassCache,
                             AdvertisementService advertisementService,
                             OnlineUserService onlineUserService,
                             ClassificationCache classificationCache,
                             SiteCacheService siteCache) {
        this.hotTagCache = hotTagCache;
        this.hotQuestionCache = hotQuestionCache;
        this.hotUserCache = hotUserCache;
        this.advertisementCache = advertisementCache;
        this.tagClassCache = tagClassCache;
        this.advertisementService = advertisementService;
        this.onlineUserService = onlineUserService;
        this.classificationCache = classificationCache;
        this.siteCache = siteCache;
    }

    @GetMapping
    public ResultDTO<Map<String, Object>> meta() {
        Map<String, Object> data = new LinkedHashMap<>();
        // 热门数据 / 静态字典走 TTL 缓存；在线人数会查库，缓存 30 秒避免每次 /api/meta 都打 MySQL
        data.put("hots", siteCache.get(SiteCacheService.META + "hots", 60_000,
                () -> hotTagCache.getHots() == null ? List.of() : hotTagCache.getHots()));
        data.put("hotQuestions", siteCache.get(SiteCacheService.META + "hotQuestions", 60_000,
                () -> hotQuestionCache.getHotQuestionDTOList()));
        data.put("hotUsers", siteCache.get(SiteCacheService.META + "hotUsers", 60_000,
                () -> hotUserCache.getUserDtoList()));
        data.put("tagCategories", siteCache.get(SiteCacheService.META + "tagCategories", 3_600_000,
                TagCache::get));
        data.put("tagClasses", siteCache.get(SiteCacheService.META + "tagClasses", 60_000,
                () -> tagClassCache.getTagClassMap() == null ? Map.of() : tagClassCache.getTagClassMap()));
        data.put("onlineCount", siteCache.get(SiteCacheService.META + "onlineCount", 30_000,
                onlineUserService::selectOnlineUserCount));

        Map<String, Object> ads = new LinkedHashMap<>();
        ads.put("home", advertisementCache.getHomeAdvertisementMap().values());
        ads.put("publish", advertisementCache.getPublishAdvertisementMap().values());
        ads.put("question", advertisementCache.getQuestionAdvertisementMap().values());
        ads.put("class", advertisementCache.getClassAdvertisementMap().values());
        ads.put("user", advertisementCache.getUserAdvertisementMap().values());
        ads.put("userHome", advertisementCache.getUserHomeAdvertisementMap().values());
        ads.put("message", advertisementCache.getMessageAdvertisementMap().values());
        ads.put("search", advertisementCache.getSearchMap().values());
        ads.put("news", advertisementCache.getNewsMap().values());
        data.put("advertisements", ads);

        data.put("classifications", siteCache.get(SiteCacheService.CLASSIFICATION + "question", 300_000,
                this::classifications));
        data.put("sorts", sorts());
        data.put("topicTypes", siteCache.get(SiteCacheService.CLASSIFICATION + "topic", 300_000,
                this::topicTypes));
        return ResultDTO.okOf(data);
    }

    /**
     * 广告点击：记录浏览量后把真实跳转地址返回给前端，由前端决定打开方式。
     */
    @GetMapping("/ad")
    public ResultDTO<Map<String, String>> adClick(@RequestParam(value = "url", defaultValue = "/") String url,
                                                  @RequestParam(value = "id", defaultValue = "-1") String id) {
        String realUrl = advertisementService.updateAdvertisementViewCount(id);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("url", realUrl.equals(url) ? realUrl : "/");
        return ResultDTO.okOf(result);
    }

    /** 问题分类（动态，管理员可在后台增删改）；value=0 是「全部」 */
    private List<Map<String, Object>> classifications() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("value", 0);
        all.put("label", "全部");
        all.put("id", 0);
        result.add(all);
        if (classificationCache.getQuestionTypes().isEmpty()) {
            for (QuestionClassType type : QuestionClassType.values()) {
                if (type.getType() != 0) result.add(option(type.getType(), type.getName(), 0L));
            }
        } else {
            for (Classification item : classificationCache.getQuestionTypes()) {
                result.add(option(item.getCode(), item.getName(), item.getId()));
            }
        }
        return result;
    }

    private List<Map<String, Object>> sorts() {
        return java.util.Arrays.stream(QuestionSortType.values())
                .map(t -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("value", t.getType());
                    item.put("label", t.getName());
                    return item;
                }).toList();
    }

    /** 话题所属分类（动态） */
    private List<Map<String, Object>> topicTypes() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        if (classificationCache.getTopicTypes().isEmpty()) {
            for (TagClassTypeEnum type : TagClassTypeEnum.values()) {
                result.add(option(type.getTypeCode(), type.getTypeName(), 0L));
            }
        } else {
            for (Classification item : classificationCache.getTopicTypes()) {
                result.add(option(item.getCode(), item.getName(), item.getId()));
            }
        }
        return result;
    }

    private Map<String, Object> option(int value, String label, Object id) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", value);
        item.put("label", label);
        item.put("id", id);
        return item;
    }
}
