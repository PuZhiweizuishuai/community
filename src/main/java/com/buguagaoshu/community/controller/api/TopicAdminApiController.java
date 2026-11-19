package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.TopicModeratorDto;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.TopicModerator;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.schedule.TagClassTasks;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TopicModerationService;
import com.buguagaoshu.community.util.AuthUtil;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.buguagaoshu.community.model.table.QuestionTableDef.QUESTION;
import static com.buguagaoshu.community.model.table.TagClassTableDef.TAG_CLASS;

/**
 * 后台「话题分类」维护接口。
 *
 * <p>可以对 topic 表中的话题进行新增 / 编辑 / 删除 / 停用，并立即刷新
 * {@code TagClassCache}，避免改动要等十分钟定时同步才生效。
 */
@RestController
@RequestMapping("/api/admin/topics")
@RequirePermission(PermissionCodes.ADMIN_TOPIC)
public class TopicAdminApiController {

    /** 只含 id 的强类型请求体，替代 Map 收参 */
    public record IdRequest(
            @NotNull(message = "话题 ID 不能为空")
            @Positive(message = "话题 ID 不合法") Long id) {
    }

    public record ModeratorRequest(
            @NotBlank(message = "用户账号不能为空") String userCode,
            String role) {
    }

    /** 卸任请求：按站点用户 ID 移除 */
    public record ModeratorRemoveRequest(
            @NotNull(message = "用户 ID 不能为空")
            @Positive(message = "用户 ID 不合法") Long userId) {
    }

    private final TagClassMapper tagClassMapper;
    private final TagClassTasks tagClassTasks;
    private final ClassificationCache classificationCache;
    private final SiteCacheService siteCache;
    private final QuestionMapper questionMapper;
    private final TopicModerationService topicModerationService;

    public TopicAdminApiController(TagClassMapper tagClassMapper, TagClassTasks tagClassTasks,
                                   ClassificationCache classificationCache,
                                   SiteCacheService siteCache,
                                   QuestionMapper questionMapper,
                                   TopicModerationService topicModerationService) {
        this.tagClassMapper = tagClassMapper;
        this.tagClassTasks = tagClassTasks;
        this.classificationCache = classificationCache;
        this.siteCache = siteCache;
        this.questionMapper = questionMapper;
        this.topicModerationService = topicModerationService;
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
        Integer typeCode = null;
        if (StringUtils.isNotBlank(type)) {
            try {
                typeCode = Integer.valueOf(type.trim());
            } catch (NumberFormatException e) {
                return ResultDTO.okOf(Map.of("topics", List.of(), "total", 0));
            }
        }
        String kw = StringUtils.trimToNull(keyword);
        List<TagClass> result = tagClassMapper.selectAdminList(typeCode, kw);
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

        TagClass sameTitle = tagClassMapper.selectOneByQuery(
                QueryWrapper.create().where(TAG_CLASS.TITLE.eq(title)));
        boolean isNew = body.getId() <= 0;
        if (sameTitle != null && (isNew || sameTitle.getId() != body.getId())) {
            return ResultDTO.errorOf(409, "话题名称「" + title + "」已存在");
        }

        if (isNew) {
            body.setCreateTime(now);
            body.setTalkCount(0);
            body.setFollowCount(0);
            tagClassMapper.insertSelective(body);
        } else {
            TagClass old = tagClassMapper.selectOneById(body.getId());
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
        siteCache.invalidate(SiteCacheService.TOPICS + "options");
        return ResultDTO.okOf(body);
    }

    /**
     * 删除话题（物理删除）。已有帖子归属该话题时禁止删除，避免外键断开。
     */
    @PostMapping("/delete")
    public ResultDTO<Map<String, Object>> delete(@Valid @RequestBody IdRequest body,
                                                 HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        long questionCount = questionMapper.selectCountByQuery(
                QueryWrapper.create().where(QUESTION.TOPIC_ID.eq(body.id())));
        if (questionCount > 0) {
            return ResultDTO.errorOf(409, "该话题下还有 " + questionCount + " 个帖子，请先移动或删除后再删除话题");
        }
        int rows = tagClassMapper.deleteById(body.id());
        tagClassTasks.TagClassCache();
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META + "tagClasses");
        siteCache.invalidate(SiteCacheService.TOPICS + "options");
        return ResultDTO.okOf(Map.of("deleted", rows > 0));
    }

    /** 话题吧务团队 */
    @GetMapping("/{id}/moderators")
    public ResultDTO<List<TopicModeratorDto>> moderators(@PathVariable("id") long id,
                                                         HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(topicModerationService.moderators(id));
    }

    /** 任命/调整吧务角色 */
    @PostMapping("/{id}/moderators")
    public ResultDTO<TopicModerator> assignModerator(@PathVariable("id") long id,
                                                     @Valid @RequestBody ModeratorRequest body,
                                                     HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(topicModerationService.assign(id, body.userCode(), body.role(), admin));
    }

    /** 卸任吧务 */
    @PostMapping("/{id}/moderators/remove")
    public ResultDTO<Object> removeModerator(@PathVariable("id") long id,
                                             @Valid @RequestBody ModeratorRemoveRequest body,
                                             HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        topicModerationService.remove(id, body.userId(), admin);
        return ResultDTO.okOf();
    }

    /** 立即把数据库里的话题同步到缓存 */
    @PostMapping("/refresh")
    public ResultDTO<Map<String, Object>> refresh(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        tagClassTasks.TagClassCache();
        siteCache.invalidate(SiteCacheService.TOPICS);
        siteCache.invalidate(SiteCacheService.META + "tagClasses");
        siteCache.invalidate(SiteCacheService.TOPICS + "options");
        return ResultDTO.okOf(Map.of("refreshed", true));
    }
}
