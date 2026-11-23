package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.TopicCreateDTO;
import com.buguagaoshu.community.dto.TopicInviteDTO;
import com.buguagaoshu.community.dto.TopicJoinDTO;
import com.buguagaoshu.community.dto.TopicMemberDto;
import com.buguagaoshu.community.dto.TopicReviewApplicationDTO;
import com.buguagaoshu.community.dto.TopicSettingsDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.TopicMemberService;
import com.buguagaoshu.community.service.TopicService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户侧话题：申请创建、我的话题、申请加入、邀请与吧务成员管理。
 */
@RestController
@RequestMapping("/api/topics")
public class TopicApiController {

    public record MemberRemoveRequest(
            @NotNull(message = "用户 ID 不能为空")
            @Positive(message = "用户 ID 不合法") Long userId) {
    }

    private final TopicService topicService;
    private final TopicMemberService topicMemberService;

    public TopicApiController(TopicService topicService, TopicMemberService topicMemberService) {
        this.topicService = topicService;
        this.topicMemberService = topicMemberService;
    }

    /** 申请创建话题：审核通过前不公开展示 */
    @PostMapping
    @RequirePermission(PermissionCodes.CONTENT_TOPIC_CREATE)
    public ResultDTO<Map<String, Object>> create(@Valid @RequestBody TopicCreateDTO body,
                                                 HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        if (!CaptchaUtil.ver(body.getCaptcha(), request)) {
            throw new CustomizeException(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }
        return ResultDTO.okOf(topicService.applyTopic(body, user));
    }

    /** 我创建的话题（含审核状态）与我加入 / 被邀请的话题 */
    @GetMapping("/mine")
    public ResultDTO<Map<String, Object>> mine(HttpServletRequest request) {
        return ResultDTO.okOf(topicService.mine(AuthUtil.requireUser(request)));
    }

    /** 申请加入私有可申请话题 */
    @PostMapping("/{id}/join")
    public ResultDTO<Object> join(@PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
                                  @Valid @RequestBody(required = false) TopicJoinDTO body,
                                  HttpServletRequest request) {
        topicMemberService.apply(id, body == null ? null : body.getMessage(),
                AuthUtil.requireUser(request));
        return ResultDTO.okOf();
    }

    /** 接受邀请 */
    @PostMapping("/{id}/invites/accept")
    public ResultDTO<Object> acceptInvite(@PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
                                          HttpServletRequest request) {
        topicMemberService.answerInvite(id, AuthUtil.requireUser(request), true);
        return ResultDTO.okOf();
    }

    /** 拒绝邀请 */
    @PostMapping("/{id}/invites/decline")
    public ResultDTO<Object> declineInvite(@PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
                                           HttpServletRequest request) {
        topicMemberService.answerInvite(id, AuthUtil.requireUser(request), false);
        return ResultDTO.okOf();
    }

    /** 成员与待审申请分页：吧务 / 吧主 / 管理员 */
    @GetMapping("/{id}/members")
    public ResultDTO<PaginationDto<TopicMemberDto>> members(
            @PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "joinType", required = false) String joinType,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        return ResultDTO.okOf(topicMemberService.listMembers(id, status, joinType, page, size,
                AuthUtil.requireUser(request)));
    }

    /** 邀请用户加入话题 */
    @PostMapping("/{id}/invites")
    public ResultDTO<Object> invite(@PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
                                    @Valid @RequestBody TopicInviteDTO body,
                                    HttpServletRequest request) {
        topicMemberService.invite(id, body.getUserCode(), AuthUtil.requireUser(request));
        return ResultDTO.okOf();
    }

    /** 审核加入申请 */
    @PostMapping("/{id}/applications/{memberId}/review")
    public ResultDTO<Object> reviewApplication(
            @PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
            @PathVariable("memberId") @Positive(message = "申请 ID 不合法") long memberId,
            @Valid @RequestBody TopicReviewApplicationDTO body,
            HttpServletRequest request) {
        topicMemberService.reviewApplication(id, memberId, Boolean.TRUE.equals(body.getApprove()),
                body.getReason(), AuthUtil.requireUser(request));
        return ResultDTO.okOf();
    }

    /** 移除成员 / 取消邀请：仅吧主与管理员 */
    @PostMapping("/{id}/members/remove")
    public ResultDTO<Object> removeMember(@PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
                                          @Valid @RequestBody MemberRemoveRequest body,
                                          HttpServletRequest request) {
        topicMemberService.removeMember(id, body.userId(), AuthUtil.requireUser(request));
        return ResultDTO.okOf();
    }

    /** 吧主修改话题简介、图标与可见性 */
    @PostMapping("/{id}/settings")
    public ResultDTO<Object> settings(@PathVariable("id") @Positive(message = "话题 ID 不合法") long id,
                                      @Valid @RequestBody TopicSettingsDTO body,
                                      HttpServletRequest request) {
        topicService.updateSettings(id, body, AuthUtil.requireUser(request));
        return ResultDTO.okOf();
    }
}
