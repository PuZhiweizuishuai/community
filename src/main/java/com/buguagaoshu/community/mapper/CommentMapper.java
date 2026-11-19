package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Comment;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
    @Insert("insert into comments(questionId , parentCommentId, parentId, type, commentator, content, likeCount, commentCount, createTime, modifiedTime, reviewStatus) " +
            "values(#{questionId}, #{parentCommentId}, #{parentId}, #{type}, #{commentator}, #{content}, #{likeCount},  #{commentCount}, #{createTime}, #{modifiedTime}, #{reviewStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "commentId", keyColumn = "commentId")
    int insertComment(Comment comment);

    default Comment selectCommentByCommentId(long commentId, int status) {
        return selectOneByQuery(QueryWrapper.create()
                .where(COMMENT.COMMENT_ID.eq(commentId)).and(COMMENT.STATUS.eq(status)));
    }

    /**
     * 条件修改删除状态：仅当当前状态符合预期时才更新，保证并发操作只有一个请求生效
     */
    @Update("update comments set status=#{status} where commentId=#{commentId} and status=#{expectedStatus}")
    int alterStatus(@Param("commentId") long commentId,
                    @Param("status") int status,
                    @Param("expectedStatus") int expectedStatus);

    @Update("update comments set commentCount=commentCount+#{commentCount} where commentId=#{commentId}")
    int updateCommentCount(Comment comment);

    /** 帖子下所有有效评论者（去重），删帖通知只需接收人 ID，不加载评论正文 */
    @Select("select distinct commentator from comments where questionId=#{questionId} and status=1 and reviewStatus=1")
    List<Long> selectDistinctCommentators(@Param("questionId") long questionId);

    /**
     * 更新点赞数（原子增量）
     */
    @Update("update comments set likeCount=likeCount+#{likeCount} where commentId=#{commentId}")
    int updateCommentLikeCount(Comment comment);

    /**
     * 审核通过：清空上一次的驳回原因与修改意见
     */
    @Update("update comments set reviewStatus=1, reviewerId=#{reviewerId}, reviewTime=#{reviewTime}, " +
            "reviewReason=null, reviewSuggestion=null where commentId=#{commentId} and reviewStatus=0")
    int approveReview(@Param("commentId") long commentId,
                      @Param("reviewerId") long reviewerId,
                      @Param("reviewTime") long reviewTime);

    /**
     * 审核驳回：记录原因、修改意见与审核人
     */
    @Update("update comments set reviewStatus=2, reviewReason=#{reason}, reviewSuggestion=#{suggestion}, " +
            "reviewerId=#{reviewerId}, reviewTime=#{reviewTime} where commentId=#{commentId} and reviewStatus=0")
    int rejectReview(@Param("commentId") long commentId,
                     @Param("reason") String reason,
                     @Param("suggestion") String suggestion,
                     @Param("reviewerId") long reviewerId,
                     @Param("reviewTime") long reviewTime);

    /**
     * 作者修改被驳回的评论并重新提交审核：更新内容、修改时间并清空上一次审核结果
     */
    @Update("update comments set content=#{content}, modifiedTime=#{modifiedTime}, reviewStatus=0, " +
            "reviewReason=null, reviewSuggestion=null, reviewerId=null, reviewTime=null " +
            "where commentId=#{commentId} and commentator=#{userId} and status=1 and reviewStatus=2")
    int resubmitReview(@Param("commentId") long commentId,
                       @Param("userId") long userId,
                       @Param("content") String content,
                       @Param("modifiedTime") long modifiedTime);
}