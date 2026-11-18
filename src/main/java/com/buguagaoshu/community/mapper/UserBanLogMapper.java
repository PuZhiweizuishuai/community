package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.UserBanLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

@Mapper
@Component(value = "UserBanLogMapper")
public interface UserBanLogMapper extends BaseMapper<UserBanLog> {

    /**
     * 解除某个用户当前生效的封禁记录。
     */
    @Update("UPDATE user_ban_logs SET status = 0, liftedTime = #{liftedTime}, liftOperatorId = #{liftOperatorId} "
            + "WHERE userId = #{userId} AND status = 1")
    int liftActive(@Param("userId") long userId,
                   @Param("liftedTime") long liftedTime,
                   @Param("liftOperatorId") long liftOperatorId);
}
