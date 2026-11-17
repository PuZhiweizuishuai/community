package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.OnlineUser;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 19:05
 */
@Mapper
public interface OnlineUserMapper extends BaseMapper<OnlineUser> {

    /**
     * 原子替换在线记录（id 为主键），替代「先查再删后插」的竞态写法
     */
    @Insert("insert into onlineUser values (#{id}, #{userName}, #{token}, #{ip}, #{time}, #{expireTime}) " +
            "on duplicate key update userName=#{userName}, token=#{token}, ip=#{ip}, time=#{time}, expireTime=#{expireTime}")
    int upsertOnlineUser(OnlineUser user);
}