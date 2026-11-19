package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.FollowTopicDTO;
import com.buguagaoshu.community.mapper.FollowTopicMapper;
import com.buguagaoshu.community.model.FollowTopic;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.FollowTopicService;
import com.buguagaoshu.community.service.TagClassService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.buguagaoshu.community.model.table.FollowTopicTableDef.FOLLOW_TOPIC;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:33
 */
@Service
public class FollowTopicServiceImpl implements FollowTopicService {
    private final FollowTopicMapper followTopicMapper;

    private final TagClassService tagClassService;

    @Autowired
    public FollowTopicServiceImpl(FollowTopicMapper followTopicMapper, TagClassService tagClassService) {
        this.followTopicMapper = followTopicMapper;
        this.tagClassService = tagClassService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertFollowTop(FollowTopicDTO followTopicDTO, long userId, TagClass tagClass) {
        FollowTopic followTopic = new FollowTopic();
        followTopic.setTopicId(tagClass.getId());
        // 标题/图标以话题表为准，关注关系不再依赖前端传入的冗余标题
        followTopic.setTopicTitle(tagClass.getTitle());
        followTopic.setTopicImage(tagClass.getImage());
        followTopic.setUserId(userId);
        followTopic.setCreateTime(System.currentTimeMillis());
        FollowTopic temp = findFollowTopic(followTopic.getTopicId(), userId);
        if (temp == null) {
            try {
                followTopicMapper.insertSelective(followTopic);
            } catch (DuplicateKeyException e) {
                // 并发重复关注：唯一索引兜底，不再累加计数
                return 1;
            }
            TagClass delta = new TagClass();
            delta.setId(tagClass.getId());
            delta.setFollowCount(1);
            tagClassService.updateFollowCount(delta);
            return 1;
        } else {
            // 先删关系，删除成功才减计数，避免并发取消导致计数多减
            int deleted = followTopicMapper.deleteById(temp.getFollowTopicId());
            if (deleted == 1) {
                TagClass delta = new TagClass();
                delta.setId(tagClass.getId());
                delta.setFollowCount(-1);
                tagClassService.updateFollowCount(delta);
            }
            return 2;
        }
    }

    @Override
    public boolean isFollowTopic(FollowTopic followTopic) {
        return findFollowTopic(followTopic.getTopicId(), followTopic.getUserId()) != null;
    }

    private FollowTopic findFollowTopic(long topicId, long userId) {
        return followTopicMapper.selectOneByQuery(QueryWrapper.create()
                .where(FOLLOW_TOPIC.TOPIC_ID.eq(topicId))
                .and(FOLLOW_TOPIC.USER_ID.eq(userId)));
    }

    @Override
    public List<FollowTopic> selectUserFollowTopic(long userId) {
        return followTopicMapper.selectListByQuery(QueryWrapper.create()
                .where(FOLLOW_TOPIC.USER_ID.eq(userId))
                .orderBy(FOLLOW_TOPIC.FOLLOW_TOPIC_ID.desc()));
    }
}