package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.UserPermission;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:12
 */
@Mapper
@Component(value = "UserPermissionMapper")
public interface UserPermissionMapper {
    /**
     * 查找用户权限
     *
     * @param id 用户id
     * @return 返回权限
     */
    @Select("select * from userPermission where id=#{id}")
    UserPermission selectUserPermissionById(long id);

    /**
     * 插入用户权限
     *
     * @param userPermission 用户权限
     * @return 结果
     */
    @Insert("insert into userPermission values (#{id}, #{power}, #{modifier}, #{updateTime}, #{dueTime});")
    int insertUserPermission(UserPermission userPermission);

    /**
     * 删除用户权限
     *
     * @param id 用户id
     * @return 结果
     */
    @Delete("delete from userPermission where id=#{id}")
    int deleteUserPermissionById(long id);

    /**
     * 修改用户权限
     *
     * @param id    用户id
     * @param power 权限值
     * @return 结果
     */
    @Update("update userPermission set power=#{power},modifier=#{modifier},updateTime=#{updateTime} where id=#{id}")
    int updateUserPermissionById(@Param("id") long id, int power, String modifier, String updateTime);

    /**
     * 修改vip到期时间
     *
     * @param id    用户id
     * @param power 权限值
     * @return 结果
     */
    @Update("update userPermission set power=#{power},modifier=#{modifier},updateTime=#{updateTime},dueTime=#{dueTime} where id=#{id}")
    int updateDueTime(@Param("id") long id, int power, String modifier, String updateTime, long dueTime);
}
