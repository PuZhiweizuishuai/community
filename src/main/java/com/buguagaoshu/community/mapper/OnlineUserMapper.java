package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.OnlineUser;
import org.apache.ibatis.annotations.*;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 19:05
 */
@Mapper
public interface OnlineUserMapper {
    /**
     * 查找当前用户是否在线
     *
     * @param id 用户id
     * @return 结果
     */
    @Select("select * from onlineUser where id=#{id}")
    OnlineUser selectOnlineUserById(long id);


    /**
     * 查找当前用户是否已经登陆
     *
     * @param token 用户token
     * @return 结果
     */
    @Select("select * from onlineUser where token=#{token}")
    OnlineUser selectOnlineUserByToken(@Param("token") String token);

    /**
     * @return 返回在线人数
     */
    @Select("SELECT COUNT(id) FROM onlineUser")
    long selectOnlineUserCount();

    /**
     * 插入在线记录
     *
     * @param user 在线用户
     * @return 结果
     */
    @Insert("insert into onlineUser values (#{id}, #{userName}, #{token}, #{ip}, #{time})")
    int insertOnlineUser(OnlineUser user);

    /**
     * 删除用户在线记录
     *
     * @param id 用户id
     * @return 结果
     */
    @Delete("delete from onlineUser where id=#{id}")
    int deleteOnlineUserById(long id);

    /**
     * 删除用户在线记录
     *
     * @param token 用户token
     * @return 结果
     */
    @Delete("delete from onlineUser where token=#{token}")
    int deleteOnlineUserByToken(@Param("token") String token);
}
