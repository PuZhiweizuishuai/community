package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.component.ViewCountBuffer;
import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionCreateDTO;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.enums.CommentSortTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.ClickLike;
import com.buguagaoshu.community.model.FollowQuestion;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ClickLikeService;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.FollowQuestionService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

/**
 * 问题详情 / 发布 / 编辑的 REST 接口（对应原 question.html、publish.html）。
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionApiController {

    private final QuestionService questionService;
    private final CommentService commentService;
    private final ClickLikeService clickLikeService;
    private final FollowQuestionService followQuestionService;
    private final SiteCacheService siteCache;
    private final ViewCountBuffer viewCountBuffer;

    public QuestionApiController(QuestionService questionService,
                                 CommentService commentService,
                                 ClickLikeService clickLikeService,
                                 FollowQuestionService followQuestionService,
                                 SiteCacheService siteCache,
                                 ViewCountBuffer viewCountBuffer) {
        this.questionService = questionService;
        this.commentService = commentService;
        this.clickLikeService = clickLikeService;
        this.followQuestionService = followQuestionService;
        this.siteCache = siteCache;
        this.viewCountBuffer = viewCountBuffer;
    }

    /**
     * 问题详情：正文、评论分页、相关问题、点赞/关注状态。
     */
    @GetMapping("/{questionId}")
    public ResultDTO<Map<String, Object>> detail(
            @PathVariable("questionId") String questionId,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "15") String size,
            @RequestParam(value = "sort", defaultValue = "0") String sort,
            HttpServletRequest request) {

        QuestionDto question = questionService.selectQuestionById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        if (question.getStatus() == 0 && AuthUtil.currentAdmin(request) == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_DELETED);
        }

        List<Question> relevant = questionService.getRelevantQuestion(question);
        PaginationDto<CommentDto> comments = commentService.getCommentDtoByQuestionIdForQuestion(
                questionId, page, size, CommentSortTypeEnum.getType(sort));
        if (question.getStatus() != 0) {
            // 浏览数先累加到 Redis，定时批量回写，读接口不再同步写库
            viewCountBuffer.increment(question.getQuestionId());
            question.setViewCount(question.getViewCount() + viewCountBuffer.pending(question.getQuestionId()));
        }

        User user = AuthUtil.currentUser(request);
        boolean liked = false;
        boolean followed = false;
        if (user != null) {
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
        data.put("question", question);
        data.put("comments", comments);
        data.put("relevantQuestions", relevant);
        data.put("isQuestionClickLike", liked);
        data.put("isFollowQuestion", followed);
        data.put("sort", sort);
        data.put("canEdit", user != null && question.getUser() != null
                && user.getId() == question.getUser().getId());
        return ResultDTO.okOf(data);
    }

    /**
     * 编辑前读取原始问题（仅作者本人）。
     */
    @GetMapping("/{questionId}/edit")
    public ResultDTO<Question> edit(@PathVariable("questionId") String questionId,
                                    HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        Question question = questionService.selectQuestionNotDtoById(questionId);
        if (question == null || question.getUserId() != user.getId()) {
            throw new CustomizeException(CustomizeErrorCode.INVALID_OPERATION);
        }
        return ResultDTO.okOf(question);
    }

    /**
     * 发布或编辑问题（questionId > 0 且 createTime 与库内一致时为编辑）。
     */
    @PostMapping
    public ResultDTO<Map<String, Object>> publish(@Valid @RequestBody QuestionCreateDTO body,
                                                  HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        String captcha = body.getCaptchaValue();
        if (!CaptchaUtil.ver(captcha, request)) {
            throw new CustomizeException(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }

        if (body.getTitle() == null || body.getTitle().isBlank() || body.getTitle().length() > 50) {
            throw new CustomizeException(CustomizeErrorCode.TITLE_INVALID);
        }
        if (body.getClassification() == null || body.getClassification().isBlank()
                || "null".equals(body.getClassification())) {
            throw new CustomizeException(CustomizeErrorCode.CLASS_INVALID);
        }
        if (body.getDescription() == null || body.getDescription().isBlank()) {
            throw new CustomizeException(CustomizeErrorCode.CONTENT_IS_EMPTY);
        }
        if (!StringUtil.judgeTagNumber(body.getTag())) {
            throw new CustomizeException(CustomizeErrorCode.TAG_LIMIT);
        }

        Question question = new Question();
        question.setQuestionId(body.getQuestionId() == null ? -1L : body.getQuestionId());
        question.setTitle(body.getTitle());
        question.setClassification(body.getClassification());
        question.setDescription(body.getDescription());
        question.setTag(body.getTag());
        question.setUserId(user.getId());
        question.setCreateTime(body.getCreateTime() == null ? 0L : body.getCreateTime());

        questionService.createQuestion(question);
        // 发布/编辑后首页列表与热门数据即时失效
        siteCache.invalidate(SiteCacheService.HOME);
        siteCache.invalidate(SiteCacheService.META + "hots");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("questionId", question.getQuestionId());
        return ResultDTO.okOf(data);
    }
}
