package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:11
 */
@Mapper
public interface CommentMapper {
    @Insert("insert into comment(questionId ,parentId, type, commentator, content, likeCount, createTime, modifiedTime) " +
            "values(#{questionId}, #{parentId}, #{type}, #{commentator}, #{content}, #{likeCount}, #{createTime}, #{modifiedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    int insertComment(Comment comment);


    @Select("select * from comment where commentId=#{commentId}")
    Comment selectCommentByCommentId(long commentId);


    @Select("select * from comment where parentId=#{parentId}")
    Comment selectCommentByParentId(long parentId);
}
