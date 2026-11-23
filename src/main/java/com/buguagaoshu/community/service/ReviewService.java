package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ReviewDetailDto;
import com.buguagaoshu.community.dto.ReviewItemDto;
import com.buguagaoshu.community.model.User;

import java.util.Map;

/**
 * 内容审核：后台审核问题、评论与用户创建的话题，用户查看/修改自己被驳回的内容。
 */
public interface ReviewService {
    /**
     * 后台审核列表。
     *
     * @param type    question / comment / topic
     * @param status  0 待审核、2 未通过、1 已通过、-1 全部
     * @param keyword 标题或内容关键字
     */
    PaginationDto<ReviewItemDto> listForAdmin(String type, String status, String keyword,
                                              String page, String size);

    /**
     * 审核详情：问题返回全部内容段落（管理员可见明文），评论返回原文与所属问题，
     * 话题返回基本信息。
     */
    ReviewDetailDto detail(String type, long id);

    /**
     * 审核通过：公开内容并执行计数、话题数、通知等发布副作用；
     * 话题通过时创建者自动成为吧主。
     */
    void approve(String type, long id, User admin);

    /**
     * 审核驳回：记录原因与修改意见，通知作者修改后重新提交。
     */
    void reject(String type, long id, String reason, String suggestion, User admin);

    /**
     * 用户「我的审核」列表。
     *
     * @param status  0 待审核、2 未通过、-1 全部（待审核 + 未通过）
     */
    PaginationDto<ReviewItemDto> listMine(long userId, String type, String status,
                                          String page, String size);

    /**
     * 作者修改被驳回的评论并重新提交审核。
     */
    void resubmitComment(long commentId, String content, User user);

    /**
     * 待审核数量统计，供后台概览使用。
     */
    Map<String, Long> pendingCounts();
}
