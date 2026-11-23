package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TopicMember;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import static com.buguagaoshu.community.model.table.TopicMemberTableDef.TOPIC_MEMBER;

/**
 * 话题成员关系。并发场景统一使用带状态条件的 CAS 更新。
 */
@Mapper
public interface TopicMemberMapper extends BaseMapper<TopicMember> {

    default TopicMember selectOne(long topicId, long userId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(TOPIC_MEMBER.TOPIC_ID.eq(topicId))
                .and(TOPIC_MEMBER.USER_ID.eq(userId)));
    }

    default List<TopicMember> selectByUser(long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(TOPIC_MEMBER.USER_ID.eq(userId))
                .orderBy(TOPIC_MEMBER.ID.desc()));
    }

    default long countByTopic(long topicId, Integer status, String joinType) {
        QueryWrapper query = QueryWrapper.create().where(TOPIC_MEMBER.TOPIC_ID.eq(topicId));
        if (status != null) {
            query.and(TOPIC_MEMBER.STATUS.eq(status));
        }
        if (joinType != null) {
            query.and(TOPIC_MEMBER.JOIN_TYPE.eq(joinType));
        }
        return selectCountByQuery(query);
    }

    default List<TopicMember> pageByTopic(long topicId, Integer status, String joinType,
                                          long offset, long size) {
        QueryWrapper query = QueryWrapper.create().where(TOPIC_MEMBER.TOPIC_ID.eq(topicId));
        if (status != null) {
            query.and(TOPIC_MEMBER.STATUS.eq(status));
        }
        if (joinType != null) {
            query.and(TOPIC_MEMBER.JOIN_TYPE.eq(joinType));
        }
        return selectListByQuery(query
                .orderBy(TOPIC_MEMBER.STATUS.asc(), TOPIC_MEMBER.ID.desc())
                .limit(offset, size));
    }

    @Update("update topic_members set status=#{status}, reviewerId=#{reviewerId}, " +
            "reviewTime=#{reviewTime} where id=#{id} and topicId=#{topicId} " +
            "and status=#{expectedStatus}")
    int updateStatusCas(@Param("id") long id,
                        @Param("topicId") long topicId,
                        @Param("status") int status,
                        @Param("expectedStatus") int expectedStatus,
                        @Param("reviewerId") long reviewerId,
                        @Param("reviewTime") long reviewTime);

    @Update("update topic_members set status=0, joinType='apply', message=#{message}, " +
            "reviewerId=null, reviewTime=null, createTime=#{createTime} " +
            "where id=#{id} and topicId=#{topicId} and userId=#{userId} and status=2")
    int reopenApply(@Param("id") long id,
                    @Param("topicId") long topicId,
                    @Param("userId") long userId,
                    @Param("message") String message,
                    @Param("createTime") long createTime);

    @Update("update topic_members set status=0, joinType='invite', message=null, " +
            "reviewerId=#{reviewerId}, reviewTime=null, createTime=#{createTime} " +
            "where id=#{id} and topicId=#{topicId} and userId=#{userId} and status=2")
    int reopenInvite(@Param("id") long id,
                     @Param("topicId") long topicId,
                     @Param("userId") long userId,
                     @Param("reviewerId") long reviewerId,
                     @Param("createTime") long createTime);

    @Update("update topic_members set status=#{status}, reviewerId=#{reviewerId}, " +
            "reviewTime=#{reviewTime} where id=#{id} and topicId=#{topicId} " +
            "and userId=#{userId} and status=0 and joinType='invite'")
    int answerInvite(@Param("id") long id,
                     @Param("topicId") long topicId,
                     @Param("userId") long userId,
                     @Param("status") int status,
                     @Param("reviewerId") long reviewerId,
                     @Param("reviewTime") long reviewTime);

    default int deleteByTopicAndUser(long topicId, long userId) {
        return deleteByQuery(QueryWrapper.create()
                .where(TOPIC_MEMBER.TOPIC_ID.eq(topicId))
                .and(TOPIC_MEMBER.USER_ID.eq(userId)));
    }
}
