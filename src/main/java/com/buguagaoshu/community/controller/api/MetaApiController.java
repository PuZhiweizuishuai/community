package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.cache.HotQuestionCache;
import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.cache.HotUserCache;
import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.TagDTO;
import com.buguagaoshu.community.dto.UserDto;
import com.buguagaoshu.community.enums.QuestionClassType;
import com.buguagaoshu.community.enums.QuestionSortType;
import com.buguagaoshu.community.enums.TagClassTypeEnum;
import com.buguagaoshu.community.model.Classification;
import com.buguagaoshu.community.model.Tag;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.AdvertisementService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionTagService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.SystemConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全站公共数据按用途拆分为多个小接口，页面按需拉取：
 * /api/meta          基础字典（分类、排序、话题分类、在线人数），启动时加载
 * /api/meta/hot      热门标签 / 热门问题（侧边栏、登录页统计）
 * /api/meta/hot-users    热门用户（仅用户列表页）
 * /api/meta/tag-categories 标签分类（仅发布页）
 * /api/meta/ads      按广告位拉取广告
 */
@RestController
@RequestMapping("/api/meta")
public class MetaApiController {

    private final HotTagCache hotTagCache;
    private final HotQuestionCache hotQuestionCache;
    private final HotUserCache hotUserCache;
    private final AdvertisementCache advertisementCache;
    private final AdvertisementService advertisementService;
    private final OnlineUserService onlineUserService;
    private final ClassificationCache classificationCache;
    private final SiteCacheService siteCache;
    private final SystemConfigService systemConfigService;
    private final TagClassCache tagClassCache;
    private final QuestionTagService questionTagService;

    public MetaApiController(HotTagCache hotTagCache,
                             HotQuestionCache hotQuestionCache,
                             HotUserCache hotUserCache,
                             AdvertisementCache advertisementCache,
                             AdvertisementService advertisementService,
                             OnlineUserService onlineUserService,
                             ClassificationCache classificationCache,
                             SiteCacheService siteCache,
                             SystemConfigService systemConfigService,
                             TagClassCache tagClassCache,
                             QuestionTagService questionTagService) {
        this.hotTagCache = hotTagCache;
        this.hotQuestionCache = hotQuestionCache;
        this.hotUserCache = hotUserCache;
        this.advertisementCache = advertisementCache;
        this.advertisementService = advertisementService;
        this.onlineUserService = onlineUserService;
        this.classificationCache = classificationCache;
        this.siteCache = siteCache;
        this.systemConfigService = systemConfigService;
        this.tagClassCache = tagClassCache;
        this.questionTagService = questionTagService;
    }

    /** 基础字典：体积小、全站通用，前端启动时拉取一次 */
    @GetMapping
    public ResultDTO<Map<String, Object>> meta() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("classifications", siteCache.get(SiteCacheService.CLASSIFICATION + "question", 300_000,
                this::classifications));
        data.put("sorts", sorts());
        data.put("topicTypes", siteCache.get(SiteCacheService.CLASSIFICATION + "topic", 300_000,
                this::topicTypes));
        // 发帖话题单选器：只下发启用中的话题精简字段，避免把简介/图标等大字段塞进基础字典
        data.put("topics", siteCache.get(SiteCacheService.TOPICS + "options", 60_000,
                this::topicOptions));
        // 在线人数会查库，缓存 30 秒避免每次请求都打 MySQL
        data.put("onlineCount", siteCache.get(SiteCacheService.META + "onlineCount", 30_000,
                onlineUserService::selectOnlineUserCount));
        // 聊天室开关：前台据此隐藏入口与路由守卫
        data.put("chatEnabled", systemConfigService.isChatEnabled());
        // 内容审核开关：发布页与评论框据此提示「发布后需审核」
        data.put("reviewEnabled", systemConfigService.isReviewEnabled());
        return ResultDTO.okOf(data);
    }

    /** 侧边栏榜单：热门标签 + 热门问题 */
    @GetMapping("/hot")
    public ResultDTO<Map<String, Object>> hot() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hots", siteCache.get(SiteCacheService.META + "hots", 60_000,
                () -> hotTagCache.getHots() == null ? List.of() : hotTagCache.getHots()));
        data.put("hotQuestions", siteCache.get(SiteCacheService.META + "hotQuestions", 60_000,
                hotQuestionCache::getHotQuestionDTOList));
        return ResultDTO.okOf(data);
    }

    /** 热门用户榜单：仅用户列表页使用 */
    @GetMapping("/hot-users")
    public ResultDTO<List<UserDto>> hotUsers() {
        return ResultDTO.okOf(siteCache.get(SiteCacheService.META + "hotUsers", 60_000,
                hotUserCache::getUserDtoList));
    }

    /** 标签建议：已通过标签按使用数排序，发布页据此展示可选标签 */
    @GetMapping("/tag-categories")
    public ResultDTO<List<TagDTO>> tagCategories() {
        return ResultDTO.okOf(siteCache.get(SiteCacheService.META + "approvedTags", 300_000,
                this::approvedTagGroups));
    }

    /** 按广告位拉取广告，position 取值见 AdvertisementCache 的位置常量对应的 key */
    @GetMapping("/ads")
    public ResultDTO<Collection<Object>> ads(@RequestParam("position") String position) {
        Map<Long, Object> ads = switch (position) {
            case "home" -> advertisementCache.getHomeAdvertisementMap();
            case "publish" -> advertisementCache.getPublishAdvertisementMap();
            case "question" -> advertisementCache.getQuestionAdvertisementMap();
            case "class" -> advertisementCache.getClassAdvertisementMap();
            case "user" -> advertisementCache.getUserAdvertisementMap();
            case "userHome" -> advertisementCache.getUserHomeAdvertisementMap();
            case "message" -> advertisementCache.getMessageAdvertisementMap();
            case "search" -> advertisementCache.getSearchMap();
            case "news" -> advertisementCache.getNewsMap();
            default -> Map.of();
        };
        return ResultDTO.okOf(ads.values());
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

    /** 发帖话题单选器数据：只包含启用中的话题 */
    private List<Map<String, Object>> topicOptions() {
        List<TagClass> topics = tagClassCache.getTagClassList();
        if (topics == null) {
            return List.of();
        }
        return topics.stream()
                .filter(topic -> topic.getStatus() == 1)
                .map(topic -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", topic.getId());
                    item.put("title", topic.getTitle());
                    item.put("type", topic.getType());
                    return item;
                }).toList();
    }

    /** 已通过标签按使用数排序，包装成发布页原有的分组结构 */
    private List<TagDTO> approvedTagGroups() {
        List<Tag> tags = questionTagService.approvedTags(60);
        if (tags.isEmpty()) {
            return List.of();
        }
        TagDTO group = new TagDTO();
        group.setCategoryName("常用标签");
        group.setTags(tags.stream().map(Tag::getName).toList());
        return List.of(group);
    }
}