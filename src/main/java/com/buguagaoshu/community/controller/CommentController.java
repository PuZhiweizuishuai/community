package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.service.CommentService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 18:56
 * 评论控制
 */
@RestController
public class CommentController {
    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/comment")
    @ResponseBody
    public Object post(@RequestBody CommentDto commentDto) {
        Comment comment = new Comment();
        comment.setQuestionId(commentDto.getQuestionId());
        comment.setParentId(commentDto.getParentId());
        comment.setType(commentDto.getType());
        comment.setCommentator(commentDto.getCommentator());
        comment.setContent(commentDto.getContent());
        comment.setCreateTime(StringUtil.getNowTime());
        comment.setModifiedTime(StringUtil.getNowTime());
        commentService.insertComment(comment);
        return ResultDTO.okOf(comment);
    }
}
