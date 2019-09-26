package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.ClickLike;
import org.apache.ibatis.annotations.*;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 16:06
 */
@Mapper
public interface ClickLikeMapper {
    /**
     * 向数据库插入点赞信息
     * @param clickLike 点赞信息
     * @return 结果
     * */
    @Insert("insert into clickLike(notifier, notifierName, receiver, questionId, commentId, type, createTime, notificationId) " +
            "values(#{notifier}, #{notifierName}, #{receiver}, #{questionId}, #{commentId}, #{type}, #{createTime}, #{notificationId})")
    @Options(useGeneratedKeys = true, keyProperty = "likeId", keyColumn = "likeId")
    int createClickLike(ClickLike clickLike);

    /**
     * 防止重复点赞
     * @param notifier 点赞人
     * @param questionId 问题ID
     * @param commentId 评论ID 如果只点赞问题，此处为 -1
     * @return ClickLike
     * */
    @Select("select * from clickLike where notifier=#{notifier} and questionId=#{questionId} and commentId=#{commentId}")
    ClickLike getClickLikePreventRepeat(@Param("notifier") long notifier, @Param("questionId") long questionId, @Param("commentId") long commentId);

    /**
     * 删除点赞
     * @param likeId 主键ID
     * @return 结果
     * */
    @Delete("DELETE FROM clickLike where likeId=#{likeId}")
    int deleteClickLike(@Param("likeId") long likeId);
}
