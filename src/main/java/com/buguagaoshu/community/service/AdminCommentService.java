package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.AdminCommentDetailDto;
import com.buguagaoshu.community.dto.AdminCommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;

/**
 * 后台评论管理：查看全部评论（含已删除）、评论上下文、删除与恢复。
 */
public interface AdminCommentService {

    /**
     * @param status  -1 全部；1 正常；0 已删除
     * @param keyword 评论内容关键字，可为空
     */
    PaginationDto<AdminCommentDto> listForAdmin(String status, String keyword, String page, String size);

    AdminCommentDetailDto detail(long commentId);

    void delete(long commentId, User admin);

    void recover(long commentId, User admin);
}
