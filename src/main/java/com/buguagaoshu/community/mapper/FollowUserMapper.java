package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.FollowUser;
import org.apache.ibatis.annotations.*;


import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 16:41
 */
@Mapper
public interface FollowUserMapper {
    /**
     * 插入关注数据
     * @param followUser 关注
     * @return 结果
     * */
    @Insert("insert into followUser(followUserId, userId, createTime) " +
            "values(#{followUserId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertFollowUser(FollowUser followUser);

    /**
     * 获取关注信息
     * @param followUser 参数
     * @return 结果
     * */
    @Select("select * from followUser where followUserId=#{followUserId} and userId=#{userId}")
    FollowUser selectFollowUser(FollowUser followUser);

    /**
     * 删除关注
     * @param followUser 关注
     * @return 结果
     * */
    @Delete("delete from followUser where id=#{id}")
    int deleteFollowUser(FollowUser followUser);


    @Select("select * from followUser where userId=#{userId} limit #{page}, #{size}")
    List<FollowUser> selectUserFollowList(@Param("userId") long userId, @Param("page") long page, @Param("size") long size);

    @Select("select COUNT(*) from followUser where userId=#{userId}")
    long selectUserFollowCount(@Param("userId") long userId);


    @Select("select * from followUser where followUserId=#{followUserId} limit #{page}, #{size}")
    List<FollowUser> selectUserFans(@Param("followUserId") long followUserId, @Param("page") long page, @Param("size") long size);


    @Select("select COUNT(*) from followUser where followUserId=#{followUserId}")
    long selectUserFansCount(@Param("followUserId") long followUserId);
}
