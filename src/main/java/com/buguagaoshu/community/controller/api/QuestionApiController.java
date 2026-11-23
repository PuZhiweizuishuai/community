package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.component.ViewCountBuffer;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionContentCreateDTO;
import com.buguagaoshu.community.dto.QuestionContentDto;
import com.buguagaoshu.community.dto.QuestionAccessDto;
import com.buguagaoshu.community.dto.QuestionContentUnlockDTO;
import com.buguagaoshu.community.dto.QuestionCreateDTO;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.enums.CommentSortTypeEnum;
import com.buguagaoshu.community.enums.QuestionContentTypeEnum;
import com.buguagaoshu.community.enums.QuestionVisibilityEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.ClickLike;
import com.buguagaoshu.community.model.FollowQuestion;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.QuestionContent;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ClickLikeService;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.FollowQuestionService;
import com.buguagaoshu.community.service.QuestionAccessService;
import com.buguagaoshu.community.service.QuestionContentService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.QuestionTagService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.service.TopicAccessService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 问题详情 / 发布 / 编辑的 REST 接口（对应原 question.html、publish.html）。
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionApiController {

    private final QuestionService questionService;
    private final QuestionContentService questionContentService;
    private final CommentService commentService;
    private final ClickLikeService clickLikeService;
    private final FollowQuestionService followQuestionService;
    private final SiteCacheService siteCache;
    private final ViewCountBuffer viewCountBuffer;
    private final QuestionTagService questionTagService;
    private final TagClassService tagClassService;
    private final ClassificationCache classificationCache;
    private final QuestionAccessService questionAccessService;
    private final TopicAccessService topicAccessService;

    public QuestionApiController(QuestionService questionService,
                                 QuestionContentService questionContentService,
                                 CommentService commentService,
                                 ClickLikeService clickLikeService,
                                 FollowQuestionService followQuestionService,
                                 SiteCacheService siteCache,
                                 ViewCountBuffer viewCountBuffer,
                                 QuestionTagService questionTagService,
                                 TagClassService tagClassService,
                                 ClassificationCache classificationCache,
                                 QuestionAccessService questionAccessService,
                                 TopicAccessService topicAccessService) {
        this.questionService = questionService;
        this.questionContentService = questionContentService;
        this.commentService = commentService;
        this.clickLikeService = clickLikeService;
        this.followQuestionService = followQuestionService;
        this.siteCache = siteCache;
        this.viewCountBuffer = viewCountBuffer;
        this.questionTagService = questionTagService;
        this.tagClassService = tagClassService;
        this.classificationCache = classificationCache;
        this.questionAccessService = questionAccessService;
        this.topicAccessService = topicAccessService;
    }

    /**
     * 问题详情：正文与点赞/关注状态。评论、相关问题由独立接口获取，
     * 评论翻页/排序时不会重复加载问题数据。
     */
    @GetMapping("/{questionId}")
    public ResultDTO<Map<String, Object>> detail(@PathVariable("questionId") String questionId,
                                                 HttpServletRequest request) {
        QuestionDto question = questionService.selectQuestionById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        User admin = AuthUtil.currentAdmin(request);
        User user = AuthUtil.currentUser(request);
        if (question.getStatus() == 0 && admin == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_DELETED);
        }
        // 待审核/未通过的问题仅作者本人与管理员可见，其他用户一律按对应状态拒绝
        if (question.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus() && admin == null
                && (user == null || user.getId() != question.getUserId())) {
            throw new CustomizeException(question.getReviewStatus() == ReviewStatusEnum.REJECTED.getStatus()
                    ? CustomizeErrorCode.CONTENT_REVIEW_REJECTED
                    : CustomizeErrorCode.CONTENT_UNDER_REVIEW);
        }

        QuestionAccessDto access = questionAccessService.resolve(toAccessQuestion(question), user, admin);
        if (!access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        if (!access.canViewContent() && !access.loginRequired()) {
            // 私有话题内容对非成员按不存在处理，避免泄露话题存在性与成员限制
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }

        if (access.canViewContent() && question.getStatus() != 0
                && question.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
            // 浏览数先累加到 Redis，定时批量回写，读接口不再同步写库
            viewCountBuffer.increment(question.getQuestionId());
            question.setViewCount(question.getViewCount() + viewCountBuffer.pending(question.getQuestionId()));
        }

        boolean liked = false;
        boolean followed = false;
        if (user != null && access.canViewContent()) {
            ClickLike clickLike = new ClickLike();
            clickLike.setNotifier(user.getId());
            clickLike.setQuestionId(question.getQuestionId());
            clickLike.setCommentId(-1);
            liked = clickLikeService.isClickLikeQuestion(clickLike);

            FollowQuestion followQuestion = new FollowQuestion();
            followQuestion.setFollowQuestionId(question.getQuestionId());
            followQuestion.setUserId(user.getId());
            followed = followQuestionService.isFollowQuestion(followQuestion);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        // 正文统一走内容段落接口，受保护段落按访问者裁剪，description 不再随详情下发
        List<QuestionContentDto> contents = access.canViewContent()
                ? questionContentService.visibleContents(
                        question.getQuestionId(), question.getUserId(), user, admin != null)
                : List.of();
        question.setDescription(null);
        boolean canEdit = user != null && question.getUser() != null
                && user.getId() == question.getUser().getId();
        // 详情页标签：访客只看到已通过标签，作者本人额外看到自己的待审核标签
        question.setTags(access.canViewContent()
                ? questionTagService.tagsForDetail(question.getQuestionId(),
                        user == null ? null : user.getId(), canEdit)
                : List.of());
        data.put("question", question);
        data.put("contents", contents);
        data.put("loginRequired", access.loginRequired());
        data.put("isQuestionClickLike", liked);
        data.put("isFollowQuestion", followed);
        data.put("canEdit", canEdit);
        return ResultDTO.okOf(data);
    }

    /** 详情 DTO 转回访问控制需要的轻量实体，避免为鉴权重复查库 */
    private Question toAccessQuestion(QuestionDto question) {
        Question accessQuestion = new Question();
        accessQuestion.setQuestionId(question.getQuestionId());
        accessQuestion.setUserId(question.getUserId());
        accessQuestion.setTopicId(question.getTopicId());
        accessQuestion.setVisibility(question.getVisibility());
        accessQuestion.setStatus(question.getStatus());
        accessQuestion.setReviewStatus(question.getReviewStatus());
        return accessQuestion;
    }

    /**
     * 问题评论分页：只查评论表，支持排序与翻页。
     */
    @GetMapping("/{questionId}/comments")
    public ResultDTO<PaginationDto<CommentDto>> comments(
            @PathVariable("questionId") String questionId,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "15") String size,
            @RequestParam(value = "sort", defaultValue = "0") String sort,
            HttpServletRequest request) {
        Question question = questionService.selectQuestionNotDtoById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        User user = AuthUtil.currentUser(request);
        User admin = AuthUtil.currentAdmin(request);
        // 未公开的问题不返回评论，避免通过评论接口绕过审核查看内容
        if (question.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()) {
            if (admin == null && (user == null || user.getId() != question.getUserId())) {
                throw new CustomizeException(CustomizeErrorCode.CONTENT_UNDER_REVIEW);
            }
        }
        requireQuestionContent(question, user, admin);
        return ResultDTO.okOf(commentService.getCommentDtoByQuestionIdForQuestion(
                questionId, page, size, CommentSortTypeEnum.getType(sort)));
    }

    /**
     * 相关问题：按问题标签匹配，随问题详情一起加载一次。
     */
    @GetMapping("/{questionId}/relevant")
    public ResultDTO<List<Question>> relevant(@PathVariable("questionId") String questionId,
                                              HttpServletRequest request) {
        Question question = questionService.selectQuestionNotDtoById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        requireQuestionContent(question, AuthUtil.currentUser(request), AuthUtil.currentAdmin(request));
        return ResultDTO.okOf(questionService.getRelevantQuestion(questionId));
    }

    /**
     * 统一内容读取守卫：不可见按 404，需登录按 403，私有话题非成员按 403。
     */
    private void requireQuestionContent(Question question, User user, User admin) {
        QuestionAccessDto access = questionAccessService.resolve(question, user, admin);
        if (!access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        if (access.loginRequired()) {
            throw new CustomizeException(CustomizeErrorCode.LOGIN_REQUIRED);
        }
        if (!access.canViewContent()) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
    }

    /**
     * 编辑前读取原始问题与全部内容段落（含明文阅读密码，仅作者本人）。
     */
    @GetMapping("/{questionId}/edit")
    @RequirePermission(PermissionCodes.CONTENT_QUESTION_PUBLISH)
    public ResultDTO<Map<String, Object>> edit(@PathVariable("questionId") String questionId,
                                               HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        Question question = questionService.selectQuestionNotDtoById(questionId);
        if (question == null || question.getUserId() != user.getId()) {
            throw new CustomizeException(CustomizeErrorCode.INVALID_OPERATION);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("question", question);
        data.put("contents", questionContentService.listByQuestionId(question.getQuestionId()));
        // 作者编辑时可见自己的待审核标签，避免保存后「标签消失」
        data.put("tags", questionTagService.tagsForDetail(question.getQuestionId(), user.getId(), true));
        return ResultDTO.okOf(data);
    }

    /**
     * 密码可见段落解锁：明文密码比对，作者本人无需密码。
     */
    @PostMapping("/{questionId}/content/unlock")
    public ResultDTO<Map<String, Object>> unlockContent(@PathVariable("questionId") String questionId,
                                                        @Valid @RequestBody QuestionContentUnlockDTO body,
                                                        HttpServletRequest request) {
        long qId;
        try {
            qId = Long.parseLong(questionId);
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        Question question = questionService.selectQuestionNotDtoById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        User user = AuthUtil.currentUser(request);
        requireQuestionContent(question, user, AuthUtil.currentAdmin(request));
        String content = questionContentService.unlock(qId, body.getContentId(), body.getPassword(),
                user, IpUtil.getIpAddr(request));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contentId", body.getContentId());
        data.put("content", content);
        return ResultDTO.okOf(data);
    }

    /**
     * 发布或编辑问题（questionId > 0 且 createTime 与库内一致时为编辑）。
     */
    @PostMapping
    @RequirePermission(PermissionCodes.CONTENT_QUESTION_PUBLISH)
    public ResultDTO<Map<String, Object>> publish(@Valid @RequestBody QuestionCreateDTO body,
                                                  HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        String captcha = body.getCaptchaValue();
        if (!CaptchaUtil.ver(captcha, request)) {
            throw new CustomizeException(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }

        List<QuestionContentCreateDTO> items = body.getContents();
        List<QuestionContent> contents = new ArrayList<>(items.size());
        for (QuestionContentCreateDTO item : items) {
            if (!QuestionContentTypeEnum.isExist(item.getType())) {
                throw new CustomizeException(CustomizeErrorCode.CONTENT_TYPE_INVALID);
            }
            QuestionContent content = new QuestionContent();
            content.setType(item.getType());
            content.setContent(item.getContent());
            if (item.getType() == QuestionContentTypeEnum.PASSWORD.getType()) {
                if (item.getPassword() == null || item.getPassword().isBlank() || item.getPassword().length() > 64) {
                    throw new CustomizeException(CustomizeErrorCode.CONTENT_PASSWORD_REQUIRED);
                }
                content.setPassword(item.getPassword());
            }
            contents.add(content);
        }
        if (!classificationCache.isValidQuestionCode(body.getClassification())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "所选问题类型不存在或已停用");
        }
        TagClass topic = tagClassService.findApprovedById(body.getTopicId());
        if (topic == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "所选话题不存在或已停用");
        }
        if (!topicAccessService.canPostInTopic(body.getTopicId(), user)) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(), "该话题仅成员可以发帖");
        }
        if (!QuestionVisibilityEnum.isValid(body.getVisibility())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "可见范围不正确");
        }
        if (body.getTagNames() != null && !body.getTagNames().isEmpty()) {
            boolean tooMany = body.getTagNames().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .count() > 5;
            if (tooMany) {
                throw new CustomizeException(CustomizeErrorCode.TAG_LIMIT);
            }
        }

        Question question = new Question();
        question.setQuestionId(body.getQuestionId() == null ? -1L : body.getQuestionId());
        question.setTitle(body.getTitle());
        question.setClassification(body.getClassification());
        question.setTopicId(body.getTopicId());
        question.setUserId(user.getId());
        question.setVisibility(body.getVisibility() == null ? 0 : body.getVisibility());
        question.setCreateTime(body.getCreateTime() == null ? 0L : body.getCreateTime());

        questionService.createQuestion(question, contents, body.getTagNames());
        // 发布/编辑后首页列表与热门数据即时失效
        siteCache.invalidate(SiteCacheService.HOME);
        siteCache.invalidate(SiteCacheService.META + "hots");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("questionId", question.getQuestionId());
        data.put("reviewStatus", question.getReviewStatus());
        return ResultDTO.okOf(data);
    }

    /**
     * 作者删除自己的问题（软删除）。
     *
     * <p>后台路径 {@code /api/admin/delete/question} 只允许管理员调用，
     * 用户自助删除走这里，所有权由 {@code QuestionService.deleteQuestion} 校验。
     */
    @DeleteMapping("/{questionId}")
    public ResultDTO<Object> deleteQuestion(@PathVariable("questionId") Long questionId,
                                            HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        questionService.deleteQuestion(questionId, user, false);
        siteCache.invalidate(SiteCacheService.HOME);
        siteCache.invalidate(SiteCacheService.META + "hots");
        return ResultDTO.okOf();
    }
}
