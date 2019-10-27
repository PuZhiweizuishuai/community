package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.FollowQuestion;
import org.apache.ibatis.annotations.*;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.Priority;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 14:43
 */
@Mapper
public interface FollowQuestionMapper {
    /**
     * 插入关注问题数据
     * @param followQuestion 关注问题
     * @return 结果
     * */
    @Insert("insert into followQuestion(followQuestionId, followQuestionAuthor, followQuestionTitle, userId, createTime) " +
            "values(#{followQuestionId}, #{followQuestionAuthor}, #{followQuestionTitle}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertFollowQuestion(FollowQuestion followQuestion);

    /**
     * 删除关注
     * @param id 关注id
     * @return 结果
     * */
    @Delete("delete from followQuestion where id=#{id}")
    int deleteFollowQuestion(@Param("id") long id);

    /**
     * 查找关注问题
     * @param followQuestion 关注问题
     * @return 结果
     * */
    @Select("select * from followQuestion where followQuestionId=#{followQuestionId} and userId=#{userId}")
    FollowQuestion selectFollowQuestion(FollowQuestion followQuestion);

    /**
     * 获取用户关注列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页显示数目
     * @return 结果
     * */
    @Select("select * from followQuestion where userId=#{userId} limit #{page}, #{size}")
    List<FollowQuestion> selectUserFollowQuestion(@Param("userId") long userId, @Param("page") long page, @Param("size") long size);

    /**
     * 获取用户关注问题数
     * @param userId 用户ID
     * @return 关注数
     */
    @Select("select COUNT(*) from followQuestion where userId=#{userId}")
    Long selectUserFollowQuestionCount(@Param("userId") long userId);
}
