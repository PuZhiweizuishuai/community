package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.CommentCreateDto;
import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.CommentService;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.buguagaoshu.community.dto.ResultDTO.errorOf;
import static com.buguagaoshu.community.dto.ResultDTO.okOf;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-31 14:22
 */
@RestController
@Slf4j
public class CommentApiController {
    private final CommentService commentService;

    @Autowired
    public CommentApiController(CommentService commentService) {
        this.commentService = commentService;
    }


    @PostMapping("/api/comment")
    public ResultDTO insertComment(@RequestBody CommentCreateDto commentDto,
                                   HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NO_LOGIN);
        }
        // 检查验证码
        if (!CaptchaUtil.ver(commentDto.getCaptcha(), request)) {
            // 清除失效的验证码
            CaptchaUtil.clear(request);
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        CaptchaUtil.clear(request);
        if (commentDto == null || commentDto.getContent() == null || commentDto.getContent().equals("") || commentDto.getContent().equals("\n")) {
            return ResultDTO.errorOf(CustomizeErrorCode.CONTENT_IS_EMPTY);
        }
        Comment comment = new Comment();
        comment.setQuestionId(commentDto.getQuestionId());
        comment.setParentId(commentDto.getParentId());
        comment.setType(commentDto.getType());
        comment.setCommentator(user.getId());
        comment.setContent(commentDto.getContent());
        comment.setCreateTime(System.currentTimeMillis());
        comment.setModifiedTime(System.currentTimeMillis());


        try {
            commentService.insertComment(comment);
        } catch (Exception e) {
            log.info("插入评论异常：{}", e.getMessage());
            return errorOf(CustomizeErrorCode.SYS_ERROR);
        }
        return okOf(comment);
    }

    @GetMapping("/api/twoLevelComment/{commentId}")
    public ResultDTO backTwoLevelComment(@PathVariable("commentId") String commentId) {
        List<CommentDto> commentDtos = commentService.getTwoLevelCommentByParent(commentId, 2);
        return ResultDTO.okOf(commentDtos);
    }
}
