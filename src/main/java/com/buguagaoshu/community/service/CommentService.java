package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.model.Comment;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:19
 */
public interface CommentService {
    int insertComment(Comment comment);

    Comment selectCommentByCommentId(long commentId);


    /**
     * 获取该问题下的所有一级评论
     * @param questionId 问题 id
     * @return 问题评论列表
     * */
    List<CommentDto> getCommentDtoByQuestionIdForQuestion(long questionId);
}
