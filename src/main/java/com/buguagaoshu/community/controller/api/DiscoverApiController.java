package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.dto.TopicModeratorDto;
import com.buguagaoshu.community.enums.TopicMemberStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.TopicMemberMapper;
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
import com.buguagaoshu.community.service.TopicAccessService;
import com.buguagaoshu.community.service.TopicModerationService;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.NumberUtils;
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
    private final TopicModerationService topicModerationService;
    private final TopicAccessService topicAccessService;
    private final TopicMemberMapper topicMemberMapper;
    private final UserBanService userBanService;

    public DiscoverApiController(QuestionService questionService,
                                 UserService userService,
                                 TagClassService tagClassService,
                                 FollowTopicService followTopicService,
                                 FollowQuestionService followQuestionService,
                                 FollowUserService followUserService,
                                 TagClassCache tagClassCache,
                                 SiteCacheService siteCache,
                                 TopicModerationService topicModerationService,
                                 TopicAccessService topicAccessService,
                                 TopicMemberMapper topicMemberMapper,
                                 UserBanService userBanService) {
        this.siteCache = siteCache;
        this.questionService = questionService;
        this.userService = userService;
        this.tagClassService = tagClassService;
        this.followTopicService = followTopicService;
        this.followQuestionService = followQuestionService;
        this.followUserService = followUserService;
        this.tagClassCache = tagClassCache;
        this.topicModerationService = topicModerationService;
        this.topicAccessService = topicAccessService;
        this.topicMemberMapper = topicMemberMapper;
        this.userBanService = userBanService;
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
     * 一次返回问题与用户两套分页结果，前端按 tab 切换；type=topic 时附带话题结果。
     */
    @GetMapping("/search")
    public ResultDTO<Map<String, Object>> search(
            @RequestParam(value = "search", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "10") String size,
            @RequestParam(value = "type", defaultValue = "question") String type,
            HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("search", keyword);
        data.put("type", type);
        if (StringUtils.isBlank(keyword)) {
            data.put("questions", new PaginationDto<QuestionDto>());
            data.put("users", new PaginationDto<User>());
            data.put("topics", new PaginationDto<TagClass>());
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
        PaginationDto<TagClass> topics = "topic".equals(type)
                ? tagClassService.searchTopics(keyword, page, size, AuthUtil.currentUser(request))
                : new PaginationDto<>();
        topics.setSearch(keyword);
        data.put("topics", topics);
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
        boolean isSelf = nowUser != null && nowUser.getId() == user.getId();
        boolean privacyHidden = !isSelf && isPrivacyHidden(user, tab);
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
        data.put("isSelf", isSelf);
        // 封禁状态：主页顶部展示「已封禁」标识，附带封禁时间与原因
        data.put("ban", userBanService.banInfo(user.getId()));
        data.put("privacyHidden", privacyHidden);
        data.put("pagination", privacyHidden ? emptyPagination(page, size) : switch (tab) {
            case "questions" -> followQuestionService.selectUserFollowQuestion(user.getId(), page, size);
            case "user" -> followUserService.getFollowUserList(user.getId(), page, size);
            case "fans" -> followUserService.getFansList(user.getId(), page, size);
            default -> questionService.getQuestionByUserId(page, size, user.getId());
        });
        // 个人主页的关注话题列表同样受话题可见性与用户隐私开关约束
        List<FollowTopic> followTopics = !privacyHidden && "topic".equals(tab)
                ? followTopicService.selectUserFollowTopic(user.getId()).stream()
                        .filter(item -> topicAccessService.resolve(item.getTopicId(), nowUser).visible())
                        .toList()
                : List.of();
        data.put("followTopics", followTopics);
        return ResultDTO.okOf(data);
    }

    /** 其他用户访问时，个人主页各子列表是否被隐私开关隐藏 */
    private boolean isPrivacyHidden(User user, String tab) {
        return switch (tab) {
            case "questions" -> user.getShowFollowQuestions() == 0;
            case "topic" -> user.getShowFollowTopics() == 0;
            case "user" -> user.getShowFollowingUsers() == 0;
            case "fans" -> user.getShowFans() == 0;
            default -> false;
        };
    }

    private PaginationDto<Object> emptyPagination(String page, String size) {
        long[] param = NumberUtils.getPageAndSize(page, size, 0);
        PaginationDto<Object> pagination = new PaginationDto<>();
        pagination.setData(List.of());
        pagination.setPagination(param[2], param[3], param[1]);
        pagination.setAllCount(0);
        return pagination;
    }

    /**
     * 话题详情（贴吧「吧」主页）：话题卡片 + 关注状态 + 吧务团队 + 当前用户吧务身份。
     * 兼容旧链接：路径不是数字时按话题名称查找。
     */
    @GetMapping("/topics/{id}")
    public ResultDTO<Map<String, Object>> topicDetail(@PathVariable("id") String id,
                                                      HttpServletRequest request) {
        TagClass tagClass;
        try {
            tagClass = tagClassCache.getById(Long.parseLong(id));
        } catch (NumberFormatException e) {
            tagClass = tagClassCache.getByTitle(id);
        }
        if (tagClass == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        User user = AuthUtil.currentUser(request);
        TopicAccessDto access = topicAccessService.resolve(tagClass.getId(), user);
        if (!access.visible()) {
            // 私有隐藏话题对非成员统一按 404 处理，避免泄露存在性
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        boolean canManage = access.isOwner() || access.isModerator() || access.isAdmin();
        List<TopicModeratorDto> moderators = topicModerationService.moderators(tagClass.getId());
        Map<String, Object> accessData = new LinkedHashMap<>();
        accessData.put("visible", access.visible());
        accessData.put("canViewContent", access.canViewContent());
        accessData.put("isMember", access.isMember());
        accessData.put("isOwner", access.isOwner());
        accessData.put("isModerator", access.isModerator());
        accessData.put("isAdmin", access.isAdmin());
        accessData.put("joinStatus", access.joinStatus());
        accessData.put("joinType", access.joinType());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tagClass", tagClass);
        data.put("followTag", isFollowed(user, tagClass.getId()));
        data.put("moderators", moderators);
        data.put("isModerator", canManage);
        data.put("access", accessData);
        data.put("pendingApplicationCount", canManage
                ? topicMemberMapper.countByTopic(tagClass.getId(),
                        TopicMemberStatusEnum.PENDING.getCode(), "apply")
                : 0);
        return ResultDTO.okOf(data);
    }

    /**
     * 话题帖子流：置顶优先，支持标签筛选。
     */
    @GetMapping("/topics/{id}/questions")
    public ResultDTO<PaginationDto<QuestionDto>> topicQuestions(
            @PathVariable("id") long id,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "15") String size,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "class", defaultValue = "0") String classification,
            HttpServletRequest request) {
        if (tagClassCache.getById(id) == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        topicAccessService.requireContentAccess(id, AuthUtil.currentUser(request));
        return ResultDTO.okOf(questionService.getTopicQuestions(id, page, size, tag,
                parseClassCode(classification)));
    }

    /** 0 或非法值表示「全部」，不参与过滤 */
    private Integer parseClassCode(String classification) {
        try {
            int code = Integer.parseInt(classification);
            return code > 0 ? code : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isFollowed(User user, long topicId) {
        if (user == null) {
            return false;
        }
        FollowTopic followTopic = new FollowTopic();
        followTopic.setUserId(user.getId());
        followTopic.setTopicId(topicId);
        return followTopicService.isFollowTopic(followTopic);
    }
}
