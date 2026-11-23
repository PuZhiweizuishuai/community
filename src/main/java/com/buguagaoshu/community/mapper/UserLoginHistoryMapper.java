package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.UserLoginHistory;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户登录历史。
 */
@Mapper
public interface UserLoginHistoryMapper extends BaseMapper<UserLoginHistory> {

    @Delete("delete from user_login_history where loginTime < #{cutoff}")
    int deleteBefore(@Param("cutoff") long cutoff);
}
