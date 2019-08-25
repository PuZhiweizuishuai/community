package com.buguagaoshu.community.service;

import com.buguagaoshu.community.model.Comment;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:19
 */
public interface CommentService {
    int insertComment(Comment comment);

    Comment selectCommentByCommentId(long commentId);

    Comment selectCommentByParentId(long parentId);
}
