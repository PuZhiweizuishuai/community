package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.FollowTopicDTO;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.FollowTopicMapper;
import com.buguagaoshu.community.model.FollowTopic;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.FollowTopicService;
import com.buguagaoshu.community.service.TagClassService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:33
 */
@Service
public class FollowTopicServiceImpl implements FollowTopicService {
    private final FollowTopicMapper followTopicMapper;

    private final TagClassService tagClassService;

    private final TagClassCache tagClassCache;

    @Autowired
    public FollowTopicServiceImpl(FollowTopicMapper followTopicMapper, TagClassService tagClassService, TagClassCache tagClassCache) {
        this.followTopicMapper = followTopicMapper;
        this.tagClassService = tagClassService;
        this.tagClassCache = tagClassCache;
    }

    @Override
    @Transactional(rollbackFor = CustomizeException.class)
    public int insertFollowTop(FollowTopicDTO followTopicDTO, Claims claims, TagClass tagClass) {
        FollowTopic followTopic = new FollowTopic();
        followTopic.setTopicId(followTopicDTO.getTopicId());
        followTopic.setTopicTitle(followTopicDTO.getTopicTitle());
        followTopic.setTopicImage(tagClass.getImage());
        followTopic.setUserId(Long.parseLong(claims.getId()));
        followTopic.setCreateTime(System.currentTimeMillis());
        FollowTopic temp = followTopicMapper.selectFollowTopic(followTopic);
        if (temp == null) {
            tagClass.setFollowCount(tagClass.getFollowCount()+1);
            tagClassCache.getTagClassMap().put(tagClass.getTitle(), tagClass);

            TagClass newClass = new TagClass();
            newClass.setId(tagClass.getId());
            newClass.setFollowCount(1);
            tagClassService.updateFollowCount(newClass);

            followTopicMapper.insertFollowTop(followTopic);
            return 1;
        } else {
            tagClass.setFollowCount(tagClass.getFollowCount()-1);
            tagClassCache.getTagClassMap().put(tagClass.getTitle(), tagClass);

            TagClass newClass = new TagClass();
            newClass.setId(tagClass.getId());
            newClass.setFollowCount(1);

            tagClassService.updateFollowCount(newClass);
            followTopicMapper.deleteFollowTopic(temp.getFollowTopicId());
            return 2;
        }
    }

    @Override
    public boolean isFollowTopic(FollowTopic followTopic) {
        FollowTopic temp = followTopicMapper.selectFollowTopic(followTopic);
        return temp != null;
    }

    @Override
    public List<FollowTopic> selectUserFollowTopic(long userId) {
        return followTopicMapper.selectUserFollowTopic(userId);
    }
}
