package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.TagAdminDto;
import com.buguagaoshu.community.model.Tag;
import com.buguagaoshu.community.model.User;

/**
 * 后台标签管理：审核、重命名与删除。
 */
public interface TagAdminService {

    PaginationDto<TagAdminDto> list(Integer status, String keyword, String page, String size);

    /** 审核通过：所有已关联帖子立即公开显示该标签 */
    Tag approve(long id, User admin);

    /** 审核拒绝：需填写原因，通知标签创建人 */
    Tag reject(long id, String reason, User admin);

    /** 重命名：同步刷新所有关联帖子的冗余标签列 */
    Tag rename(long id, String name, User admin);

    /** 删除：先解除与帖子的关联并刷新冗余列 */
    void delete(long id, User admin);
}
