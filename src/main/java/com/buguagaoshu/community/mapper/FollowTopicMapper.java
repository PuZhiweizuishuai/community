package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.FollowTopic;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:09
 */
@Mapper
public interface FollowTopicMapper {
    /**
     * 插入关注话题
     * @param followTopic 关注话题
     * @return 结果
     * */
    @Insert("insert into followTopic(topicId, topicTitle, topicImage, userId, createTime) " +
            "values(#{topicId}, #{topicTitle}, #{topicImage}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "followTopicId", keyColumn = "followTopicId")
    int insertFollowTop(FollowTopic followTopic);

    /**
     * 获取用户关注列表
     * @param userId 用户 Id
     * @return 关注列表
     * */
    @Select("select * from followTopic where userId=#{userId}")
    List<FollowTopic> selectUserFollowTopic(@Param("userId") long userId);

    /**
     * 获取关注话题
     * @param followTopic 话题
     * @return 结果
     * */
    @Select("select * from followTopic where topicId=#{topicId} and userId=#{userId}")
    FollowTopic selectFollowTopic(FollowTopic followTopic);

    /**
     * 删除关注
     * @param followTopicId 关注id
     * @return 结果
     * */
    @Delete("delete from followTopic where followTopicId=#{followTopicId}")
    int deleteFollowTopic(@Param("followTopicId") long followTopicId);
}
