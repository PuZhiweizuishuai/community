package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.UserLoginHistoryMapper;
import com.buguagaoshu.community.model.UserLoginHistory;
import com.buguagaoshu.community.service.UserLoginHistoryService;
import com.buguagaoshu.community.util.NumberUtils;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.buguagaoshu.community.model.table.UserLoginHistoryTableDef.USER_LOGIN_HISTORY;

/**
 * 登录历史写入与查询。审计数据属于旁路能力，写入失败不能阻断登录。
 */
@Service
@Slf4j
public class UserLoginHistoryServiceImpl implements UserLoginHistoryService {

    private static final int IP_MAX_LENGTH = 64;
    private static final int UA_MAX_LENGTH = 512;

    private final UserLoginHistoryMapper userLoginHistoryMapper;

    public UserLoginHistoryServiceImpl(UserLoginHistoryMapper userLoginHistoryMapper) {
        this.userLoginHistoryMapper = userLoginHistoryMapper;
    }

    @Override
    public void record(long userId, String ip, String userAgent, int loginType) {
        if (userId <= 0) {
            return;
        }
        try {
            UserLoginHistory history = new UserLoginHistory();
            history.setUserId(userId);
            history.setLoginTime(System.currentTimeMillis());
            history.setIp(StringUtils.truncate(StringUtils.defaultString(ip), IP_MAX_LENGTH));
            history.setUserAgent(StringUtils.truncate(StringUtils.defaultString(userAgent), UA_MAX_LENGTH));
            history.setLoginType(loginType);
            userLoginHistoryMapper.insertSelective(history);
        } catch (Exception e) {
            log.warn("记录用户 {} 登录历史失败：{}", userId, e.getMessage());
        }
    }

    @Override
    public PaginationDto<UserLoginHistory> pageByUser(long userId, String page, String size) {
        QueryWrapper query = QueryWrapper.create().where(USER_LOGIN_HISTORY.USER_ID.eq(userId));
        long total = userLoginHistoryMapper.selectCountByQuery(query);
        long[] param = NumberUtils.getPageAndSize(page, size, total);
        List<UserLoginHistory> list = userLoginHistoryMapper.selectListByQuery(
                query.orderBy(USER_LOGIN_HISTORY.LOGIN_TIME.desc(), USER_LOGIN_HISTORY.ID.desc())
                        .limit(param[0], param[1]));
        PaginationDto<UserLoginHistory> paginationDto = new PaginationDto<>();
        paginationDto.setData(list);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(total);
        return paginationDto;
    }

    @Override
    public int cleanupBefore(long cutoff) {
        return userLoginHistoryMapper.deleteBefore(cutoff);
    }
}
