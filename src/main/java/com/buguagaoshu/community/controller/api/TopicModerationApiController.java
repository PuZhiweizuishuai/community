package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.TopicModerationService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 话题吧务操作接口。所有接口都先校验当前用户是该话题吧务，
 * 再校验目标帖子属于该话题，防止跨吧操作。
 */
@RestController
@RequestMapping("/api/topics/{topicId}/moderation")
public class TopicModerationApiController {

    public record QuestionRequest(
            @NotNull(message = "问题 ID 不能为空")
            @Positive(message = "问题 ID 不合法") Long questionId) {
    }

    public record ToggleRequest(
            @NotNull(message = "问题 ID 不能为空")
            @Positive(message = "问题 ID 不合法") Long questionId,
            boolean enabled) {
    }

    public record TagsRequest(
            @NotNull(message = "问题 ID 不能为空")
            @Positive(message = "问题 ID 不合法") Long questionId,
            @Size(max = 5, message = "最多只能添加 5 个标签") List<String> tagNames) {
    }

    private final TopicModerationService topicModerationService;

    public TopicModerationApiController(TopicModerationService topicModerationService) {
        this.topicModerationService = topicModerationService;
    }

    @PostMapping("/delete")
    public ResultDTO<Object> delete(@PathVariable("topicId") long topicId,
                                    @Valid @RequestBody QuestionRequest body,
                                    HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        topicModerationService.deleteQuestion(topicId, body.questionId(), user);
        return ResultDTO.okOf();
    }

    @PostMapping("/recover")
    public ResultDTO<Object> recover(@PathVariable("topicId") long topicId,
                                     @Valid @RequestBody QuestionRequest body,
                                     HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        topicModerationService.recoverQuestion(topicId, body.questionId(), user);
        return ResultDTO.okOf();
    }

    @PostMapping("/top")
    public ResultDTO<Object> top(@PathVariable("topicId") long topicId,
                                 @Valid @RequestBody ToggleRequest body,
                                 HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        topicModerationService.setTop(topicId, body.questionId(), body.enabled(), user);
        return ResultDTO.okOf();
    }

    @PostMapping("/essence")
    public ResultDTO<Object> essence(@PathVariable("topicId") long topicId,
                                     @Valid @RequestBody ToggleRequest body,
                                     HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        topicModerationService.setEssence(topicId, body.questionId(), body.enabled(), user);
        return ResultDTO.okOf();
    }

    @PostMapping("/tags")
    public ResultDTO<Object> tags(@PathVariable("topicId") long topicId,
                                  @Valid @RequestBody TagsRequest body,
                                  HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        topicModerationService.updateTags(topicId, body.questionId(), body.tagNames(), user);
        return ResultDTO.okOf();
    }
}
