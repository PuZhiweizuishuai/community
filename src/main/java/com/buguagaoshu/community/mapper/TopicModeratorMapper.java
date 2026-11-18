package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TopicModerator;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

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

    default TopicModerator selectOne(long topicId, long userId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(TOPIC_MODERATOR.TOPIC_ID.eq(topicId))
                .and(TOPIC_MODERATOR.USER_ID.eq(userId)));
    }
}
