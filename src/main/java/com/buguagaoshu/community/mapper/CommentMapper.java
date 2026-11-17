package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Comment;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

import static com.buguagaoshu.community.model.table.CommentTableDef.COMMENT;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 19:11
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 插入评论。status 等列依赖数据库默认值，不能按实体全字段插入，保留显式列 SQL。
     */
    @Insert("insert into comment(questionId , parentCommentId, parentId, type, commentator, content, likeCount, commentCount, createTime, modifiedTime) " +
            "values(#{questionId}, #{parentCommentId}, #{parentId}, #{type}, #{commentator}, #{content}, #{likeCount},  #{commentCount}, #{createTime}, #{modifiedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "commentId", keyColumn = "commentId")
    int insertComment(Comment comment);

    default Comment selectCommentByCommentId(long commentId, int status) {
        return selectOneByQuery(QueryWrapper.create()
                .where(COMMENT.COMMENT_ID.eq(commentId)).and(COMMENT.STATUS.eq(status)));
    }

    @Update("update comment set commentCount=commentCount+#{commentCount} where commentId=#{commentId}")
    int updateCommentCount(Comment comment);

    /**
     * 更新点赞数（原子增量）
     */
    @Update("update comment set likeCount=likeCount+#{likeCount} where commentId=#{commentId}")
    int updateCommentLikeCount(Comment comment);
}