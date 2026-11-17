package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.IndexTopQuestion;
import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.enums.QuestionClassType;
import com.buguagaoshu.community.enums.QuestionSortType;
import com.buguagaoshu.community.model.FollowTopic;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.FollowTopicService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页数据接口（对应原 / 与 /index 的 Thymeleaf 模型数据）。
 */
@RestController
@RequestMapping("/api/home")
public class HomeApiController {

    private final QuestionService questionService;
    private final IndexTopQuestion indexTopQuestion;
    private final TagClassCache tagClassCache;
    private final FollowTopicService followTopicService;
    private final SiteCacheService siteCache;

    public HomeApiController(QuestionService questionService,
                             IndexTopQuestion indexTopQuestion,
                             TagClassCache tagClassCache,
                             FollowTopicService followTopicService,
                             SiteCacheService siteCache) {
        this.questionService = questionService;
        this.indexTopQuestion = indexTopQuestion;
        this.tagClassCache = tagClassCache;
        this.followTopicService = followTopicService;
        this.siteCache = siteCache;
    }

    @GetMapping
    public ResultDTO<Map<String, Object>> home(
            HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "15") String size,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "sort", defaultValue = "3") String sort,
            @RequestParam(value = "class", defaultValue = "0") String classification,
            @RequestParam(value = "cursor", required = false) String cursor) {

        int sortValue = parseInt(sort, QuestionSortType.NEW_QUESTION.getType());
        int classValue = parseInt(classification, QuestionClassType.ALL.getType());

        // 列表、置顶、话题卡片是公共数据，按查询条件缓存 20 秒；
        // followTag 与登录用户相关，必须每次请求单独计算，不能进缓存。
        String key = SiteCacheService.HOME + String.join("|", page, size,
                tag == null ? "" : tag, sort, classification, cursor == null ? "" : cursor);
        Map<String, Object> data = new LinkedHashMap<>(siteCache.get(key, 20_000,
                () -> buildPublicData(page, size, tag, sort, classification, sortValue, classValue, cursor)));

        TagClass tagClass = (TagClass) data.get("tagClass");
        boolean followed = false;
        if (tagClass != null) {
            User user = AuthUtil.currentUser(request);
            if (user != null) {
                FollowTopic followTopic = new FollowTopic();
                followTopic.setUserId(user.getId());
                followTopic.setTopicId(tagClass.getId());
                followed = followTopicService.isFollowTopic(followTopic);
            }
        }
        data.put("followTag", followed);
        return ResultDTO.okOf(data);
    }

    private Map<String, Object> buildPublicData(String page, String size, String tag,
                                                String sort, String classification,
                                                int sortValue, int classValue, String cursor) {
        Map<String, Object> data = new LinkedHashMap<>();
        PaginationDto<QuestionDto> pagination = questionService.getSomeQuestionDto(
                page, size, tag, sortValue, classValue, cursor);
        data.put("pagination", pagination);
        data.put("tag", tag);
        data.put("sort", sort);
        data.put("class", classification);

        boolean defaultPage = "1".equals(page) && (tag == null || tag.isBlank())
                && "3".equals(sort) && "0".equals(classification);
        data.put("topQuestions", defaultPage ? indexTopQuestion.getTopQuestion() : List.of());
        // 缓存快照是不可变 Map，get(null) 会 NPE，先判空
        Map<String, TagClass> tagMap = tagClassCache.getTagClassMap();
        data.put("tagClass", tagMap == null || tag == null ? null : tagMap.get(tag));
        return data;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
