package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.FollowTopicDTO;
import com.buguagaoshu.community.model.FollowTopic;
import com.buguagaoshu.community.model.TagClass;
import io.jsonwebtoken.Claims;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:32
 */
public interface FollowTopicService {
    /**
     * 插入关注话题
     * @param followTopicDTO 关注话题
     * @param claims 用户信息
     * @param tagClass 话题信息
     * @return 结果
     * */
    int insertFollowTop(FollowTopicDTO followTopicDTO,  Claims claims, TagClass tagClass);

    /**
     * 有没有关注这个话题
     * @param followTopic 话题
     * @return 关注 true， 没关注 false
     * */
    boolean isFollowTopic(FollowTopic followTopic);

    /**
     * 获取用户关注列表
     * @param userId 用户 Id
     * @return 关注列表
     * */
    List<FollowTopic> selectUserFollowTopic(long userId);
}
