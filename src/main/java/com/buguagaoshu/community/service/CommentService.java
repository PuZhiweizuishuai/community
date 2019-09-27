package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.CommentSortTypeEnum;
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
     * @param page 页码
     * @param size 每页显示评论数
     * @param sort 排序方式
     * @return 问题评论列表
     * */
    PaginationDto<CommentDto> getCommentDtoByQuestionIdForQuestion(String questionId, String page, String size, CommentSortTypeEnum commentSortTypeEnum);


    /**
     * 返回二级评论
     * */
    List<CommentDto> getTwoLevelCommentByParent(String parentId, int type);
}
