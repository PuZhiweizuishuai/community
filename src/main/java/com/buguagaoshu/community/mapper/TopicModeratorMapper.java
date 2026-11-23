package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TopicModerator;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import static com.buguagaoshu.community.model.table.TopicModeratorTableDef.TOPIC_MODERATOR;

/**
 * 话题吧务成员读写。
 */
@Mapper
public interface TopicModeratorMapper extends BaseMapper<TopicModerator> {

    default List<TopicModerator> selectByTopicId(long topicId) {
        return selectListByQuery(QueryWrapper.create()
                .where(TOPIC_MODERATOR.TOPIC_ID.eq(topicId))
                .orderBy(TOPIC_MODERATOR.ROLE.asc(), TOPIC_MODERATOR.ID.asc()));
    }

    default List<TopicModerator> selectByUserId(long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(TOPIC_MODERATOR.USER_ID.eq(userId)));
    }

    default List<TopicModerator> selectOwners(long topicId) {
        return selectListByQuery(QueryWrapper.create()
                .where(TOPIC_MODERATOR.TOPIC_ID.eq(topicId))
                .and(TOPIC_MODERATOR.ROLE.eq(TopicModerator.ROLE_OWNER))
                .orderBy(TOPIC_MODERATOR.ID.asc()));
    }

    default TopicModerator selectOne(long topicId, long userId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(TOPIC_MODERATOR.TOPIC_ID.eq(topicId))
                .and(TOPIC_MODERATOR.USER_ID.eq(userId)));
    }

    /** 卸任：直接按话题 + 用户删除，省去先查后删 */
    default int deleteByTopicAndUser(long topicId, long userId) {
        return deleteByQuery(QueryWrapper.create()
                .where(TOPIC_MODERATOR.TOPIC_ID.eq(topicId))
                .and(TOPIC_MODERATOR.USER_ID.eq(userId)));
    }

    /** 任命新吧主前把旧吧主降级为小吧，保证每个话题只有一个吧主 */
    @Update("update topic_moderators set role='moderator' where topicId=#{topicId} and role='owner'")
    int demoteOwners(@Param("topicId") long topicId);
}
