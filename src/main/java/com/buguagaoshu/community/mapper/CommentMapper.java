package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:11
 */
@Mapper
public interface CommentMapper {
    @Insert("insert into comment(questionId , parentCommentId, parentId, type, commentator, content, likeCount, commentCount, createTime, modifiedTime) " +
            "values(#{questionId}, #{parentCommentId}, #{parentId}, #{type}, #{commentator}, #{content}, #{likeCount},  #{commentCount}, #{createTime}, #{modifiedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "commentId", keyColumn = "commentId")
    int insertComment(Comment comment);


    @Select("select * from comment where commentId=#{commentId} AND status=#{status}")
    Comment selectCommentByCommentId(long commentId, @Param("status") int status);

    /**
     * 查找一级评论列表
     * 按时间正序
     */
    @Select("select * from comment where questionId=#{questionId} AND type=#{type} AND status=#{status} limit #{page}, #{size}")
    List<Comment> getCommentDtoByQuestionId(@Param("questionId") long questionId, @Param("type") int type, @Param("page") long page, @Param("size") long size, @Param("status") int status);

    /**
     * 查找一级评论列表
     * 按时间倒序
     */
    @Select("select * from comment where questionId=#{questionId} AND type=#{type} AND status=#{status} ORDER BY createTime DESC limit #{page}, #{size}")
    List<Comment> getNewCommentDtoByQuestionId(@Param("questionId") long questionId, @Param("type") int type, @Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * 查找一级评论列表
     * 按点赞数排序
     */
    @Select("select * from comment where questionId=#{questionId} AND type=#{type} AND status=#{status} ORDER BY likeCount DESC limit #{page}, #{size}")
    List<Comment> getLikeCountCommentDtoByQuestionId(@Param("questionId") long questionId, @Param("type") int type, @Param("page") long page, @Param("size") long size, @Param("status") int status);

    /**
     * 查找一级评论列表
     * 按回复数排序
     */
    @Select("select * from comment where questionId=#{questionId} AND type=#{type} AND status=#{status} ORDER BY commentCount DESC limit #{page}, #{size}")
    List<Comment> getReplyCountCommentDtoByQuestionId(@Param("questionId") long questionId, @Param("type") int type, @Param("page") long page, @Param("size") long size, @Param("status") int status);



    @Select("select COUNT(1) from comment where questionId=#{questionId} AND type=#{type} AND status=#{status}")
    long getCommentNumber(@Param("questionId") long questionId, @Param("type") int type, @Param("status") int status);


    /**
     * 查找二级评论列表
     */
    @Select("select * from comment where parentCommentId=#{parentCommentId} AND type=#{type} AND status=#{status}")
    List<Comment> getTwoLevelCommentByParent(@Param("parentCommentId") long parentCommentId, @Param("type") int type, @Param("status") int status);


    @Update("update comment set commentCount=commentCount+#{commentCount} where commentId=#{commentId}")
    int updateCommentCount(Comment comment);

    /**
     * 更新点赞数
     * */
    @Update("update comment set likeCount=likeCount+#{likeCount} where commentId=#{commentId}")
    int updateCommentLikeCount(Comment comment);


    @Select("select * from comment where questionId=#{questionId} AND status=#{status}")
    List<Comment> getAllComment(@Param("questionId") long questionId, @Param("status") int status);
}
