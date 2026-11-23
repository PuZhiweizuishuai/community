package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.UserLoginHistory;

/**
 * 用户登录历史。
 */
public interface UserLoginHistoryService {

    /**
     * 记录一次成功登录。写入失败只记录日志，不影响登录结果。
     */
    void record(long userId, String ip, String userAgent, int loginType);

    PaginationDto<UserLoginHistory> pageByUser(long userId, String page, String size);

    int cleanupBefore(long cutoff);
}
