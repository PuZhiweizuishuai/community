package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.CommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.CommentSortTypeEnum;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.User;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:19
 */
public interface CommentService {
    /**
     * 新增评论，返回可直接渲染的评论数据。
     * 开启内容审核时评论先进入待审核状态，不计数、不发回复通知。
     * @param comment 待插入评论
     * @return 带作者信息的评论
     */
    CommentDto insertComment(Comment comment);

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
     * 分页返回某条一级评论下的回复（二级回复统一归属根评论）
     * @param parentId 根评论 ID
     * @param page 页码
     * @param size 每页显示数量
     * @return 回复分页，没有回复时返回空分页
     * */
    PaginationDto<CommentDto> getTwoLevelCommentByParent(String parentId, String page, String size);

    /**
     * 审核通过后执行发布副作用：问题/根评论计数 +1，并向被回复人发送回复通知。
     * 由审核服务在通过审核时调用；审核关闭时由 insertComment 直接调用。
     * @param comment 已通过审核的评论
     */
    void publishApprovedComment(Comment comment);

    /**
     * 作者删除自己的评论（软删除）。
     *
     * <p>删除后：已通过审核的评论回退问题/根评论的回复数；待审核或被驳回的评论
     * 本来就没有计数，直接置为删除状态即可。所有权校验在服务层完成。
     *
     * @param commentId 评论 ID
     * @param user      当前登录用户
     */
    void deleteOwnComment(long commentId, User user);
}
