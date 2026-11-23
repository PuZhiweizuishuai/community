package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.CommentCreateDto;
import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionAccessDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.QuestionAccessService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import static com.buguagaoshu.community.dto.ResultDTO.okOf;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-31 14:22
 */
@RestController
@Slf4j
public class CommentApiController {
    private final CommentService commentService;
    private final SiteCacheService siteCache;
    private final QuestionMapper questionMapper;
    private final CommentMapper commentMapper;
    private final QuestionAccessService questionAccessService;

    @Autowired
    public CommentApiController(CommentService commentService, SiteCacheService siteCache,
                                QuestionMapper questionMapper, CommentMapper commentMapper,
                                QuestionAccessService questionAccessService) {
        this.commentService = commentService;
        this.siteCache = siteCache;
        this.questionMapper = questionMapper;
        this.commentMapper = commentMapper;
        this.questionAccessService = questionAccessService;
    }


    @PostMapping("/api/comment")
    @RequirePermission(PermissionCodes.CONTENT_COMMENT_CREATE)
    public ResultDTO<CommentDto> insertComment(@Valid @RequestBody CommentCreateDto commentDto,
                                               HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        // 检查验证码（校验即清除）
        if (!CaptchaUtil.ver(commentDto.getCaptcha(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        Question question = questionMapper.selectQuestionById(commentDto.getQuestionId(), 1);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        requireQuestionContent(question, request);

        Comment comment = new Comment();
        comment.setQuestionId(commentDto.getQuestionId());
        comment.setParentId(commentDto.getParentId());
        comment.setType(commentDto.getType());
        comment.setCommentator(user.getId());
        comment.setContent(commentDto.getContent().trim());
        comment.setCreateTime(System.currentTimeMillis());
        comment.setModifiedTime(System.currentTimeMillis());

        CommentDto created = commentService.insertComment(comment);
        // 评论数变化会影响首页列表与热门排序
        siteCache.invalidate(SiteCacheService.HOME);
        return okOf(created);
    }

    @GetMapping("/api/twoLevelComment/{commentId}")
    public ResultDTO<PaginationDto<CommentDto>> backTwoLevelComment(
            @PathVariable("commentId") String commentId,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "10") String size,
            HttpServletRequest request) {
        long id;
        try {
            id = Long.parseLong(commentId);
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
        }
        Comment comment = commentMapper.selectOneById(id);
        if (comment == null) {
            throw new CustomizeException(CustomizeErrorCode.COMMENT_NOT_FOUND);
        }
        Question question = questionMapper.selectQuestionById(comment.getQuestionId(), 1);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        requireQuestionContent(question, request);
        return ResultDTO.okOf(commentService.getTwoLevelCommentByParent(commentId, page, size));
    }

    /** 评论读写前统一校验问题内容权限，防止通过评论 ID 绕过话题/登录可见限制 */
    private void requireQuestionContent(Question question, HttpServletRequest request) {
        QuestionAccessDto access = questionAccessService.resolve(question,
                AuthUtil.currentUser(request), AuthUtil.currentAdmin(request));
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
     * 作者删除自己的评论（软删除）。所有权由服务层校验，
     * 管理员删除评论走 {@code /api/admin/comments/{id}/delete}。
     */
    @DeleteMapping("/api/comment/{commentId}")
    public ResultDTO<Object> deleteComment(@PathVariable("commentId") @Positive(message = "评论 ID 不合法") long commentId,
                                          HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        commentService.deleteOwnComment(commentId, user);
        siteCache.invalidate(SiteCacheService.HOME);
        return ResultDTO.okOf();
    }
}
