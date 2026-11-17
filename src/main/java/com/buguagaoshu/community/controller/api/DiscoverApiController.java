package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.FollowTopic;
import com.buguagaoshu.community.model.FollowUser;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.FollowQuestionService;
import com.buguagaoshu.community.service.FollowTopicService;
import com.buguagaoshu.community.service.FollowUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发现类页面数据：话题分类、搜索、用户列表、个人主页。
 * 侧边栏公共数据由 /api/meta 提供。
 */
@RestController
@RequestMapping("/api")
public class DiscoverApiController {

    private final QuestionService questionService;
    private final UserService userService;
    private final TagClassService tagClassService;
    private final FollowTopicService followTopicService;
    private final FollowQuestionService followQuestionService;
    private final FollowUserService followUserService;
    private final TagClassCache tagClassCache;
    private final SiteCacheService siteCache;

    public DiscoverApiController(QuestionService questionService,
                                 UserService userService,
                                 TagClassService tagClassService,
                                 FollowTopicService followTopicService,
                                 FollowQuestionService followQuestionService,
                                 FollowUserService followUserService,
                                 TagClassCache tagClassCache,
                                 SiteCacheService siteCache) {
        this.siteCache = siteCache;
        this.questionService = questionService;
        this.userService = userService;
        this.tagClassService = tagClassService;
        this.followTopicService = followTopicService;
        this.followQuestionService = followQuestionService;
        this.followUserService = followUserService;
        this.tagClassCache = tagClassCache;
    }

    /**
     * 话题分类页（原 /class）。
     */
    @GetMapping("/topics")
    public ResultDTO<Map<String, Object>> topics(
            @RequestParam(value = "type", defaultValue = "1") String type) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("tags", siteCache.get(SiteCacheService.TOPICS + type, 60_000,
                () -> tagClassService.getTagClassByTypeNotUserPage(type)));
        return ResultDTO.okOf(data);
    }

    /**
     * 搜索页（原 /search/{keyword}、/search/{keyword}/user）。
     * 一次返回问题与用户两套分页结果，前端按 tab 切换。
     */
    @GetMapping("/search")
    public ResultDTO<Map<String, Object>> search(
            @RequestParam(value = "search", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "10") String size,
            @RequestParam(value = "type", defaultValue = "question") String type) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("search", keyword);
        data.put("type", type);
        if (StringUtils.isBlank(keyword)) {
            data.put("questions", new PaginationDto<QuestionDto>());
            data.put("users", new PaginationDto<User>());
            return ResultDTO.okOf(data);
        }
        // LIKE 搜索：关键字长度与通配符清洗在 Service 内完成
        PaginationDto<QuestionDto> questions = questionService.searchQuestion(keyword, page, size);
        questions.setSearch(keyword);
        PaginationDto<User> users = userService.searchUser(keyword, page,
                "user".equals(type) ? size : "5");
        users.setSearch(keyword);
        data.put("questions", questions);
        data.put("users", users);
        return ResultDTO.okOf(data);
    }

    /**
     * 用户列表页（原 /user）。
     */
    @GetMapping("/users")
    public ResultDTO<Map<String, Object>> userList(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "10") String size,
            @RequestParam(value = "class", defaultValue = "0") String type) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", userService.getUserList(page, size));
        data.put("class", type);
        data.put("page", page);
        return ResultDTO.okOf(data);
    }

    /**
     * 个人主页（原 /user/{userId} 及 4 个 following 子页）。
     *
     * @param tab posts | questions | topic | user | fans
     */
    @GetMapping("/users/{userId}")
    public ResultDTO<Map<String, Object>> userHome(
            @PathVariable("userId") String userId,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "15") String size,
            @RequestParam(value = "tab", defaultValue = "posts") String tab,
            HttpServletRequest request) {
        User user = userService.selectUserByUserId(userId);
        if (user == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        user.clean();

        User nowUser = AuthUtil.currentUser(request);
        boolean isFollowUser = false;
        if (nowUser != null) {
            FollowUser followUser = new FollowUser();
            followUser.setFollowUserId(user.getId());
            followUser.setUserId(nowUser.getId());
            isFollowUser = followUserService.isFollowUser(followUser);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", user);
        data.put("tab", tab);
        data.put("isFollowUser", isFollowUser);
        data.put("isSelf", nowUser != null && nowUser.getId() == user.getId());
        data.put("pagination", switch (tab) {
            case "questions" -> followQuestionService.selectUserFollowQuestion(user.getId(), page, size);
            case "user" -> followUserService.getFollowUserList(user.getId(), page, size);
            case "fans" -> followUserService.getFansList(user.getId(), page, size);
            default -> questionService.getQuestionByUserId(page, size, user.getId());
        });
        List<FollowTopic> followTopics = followTopicService.selectUserFollowTopic(user.getId());
        data.put("followTopics", "topic".equals(tab) ? followTopics : List.<FollowTopic>of());
        return ResultDTO.okOf(data);
    }

    /**
     * 话题详情（首页按标签筛选时右侧的话题卡片数据）。
     */
    @GetMapping("/topics/{title}")
    public ResultDTO<Map<String, Object>> topicDetail(@PathVariable("title") String title,
                                                      HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        TagClass tagClass = tagClassCache.getTagClassMap() == null
                ? null : tagClassCache.getTagClassMap().get(title);
        data.put("tagClass", tagClass);
        User user = AuthUtil.currentUser(request);
        boolean followed = false;
        if (tagClass != null && user != null) {
            FollowTopic followTopic = new FollowTopic();
            followTopic.setUserId(user.getId());
            followTopic.setTopicId(tagClass.getId());
            followed = followTopicService.isFollowTopic(followTopic);
        }
        data.put("followTag", followed);
        return ResultDTO.okOf(data);
    }
}
