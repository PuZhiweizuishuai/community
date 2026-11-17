package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.CommentCreateDto;
import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

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

    @Autowired
    public CommentApiController(CommentService commentService, SiteCacheService siteCache) {
        this.commentService = commentService;
        this.siteCache = siteCache;
    }


    @PostMapping("/api/comment")
    public ResultDTO<CommentDto> insertComment(@Valid @RequestBody CommentCreateDto commentDto,
                                               HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        // 检查验证码（校验即清除）
        if (!CaptchaUtil.ver(commentDto.getCaptcha(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }

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
    public ResultDTO backTwoLevelComment(@PathVariable("commentId") String commentId) {
        List<CommentDto> commentDtos = commentService.getTwoLevelCommentByParent(commentId);
        return ResultDTO.okOf(commentDtos);
    }
}
