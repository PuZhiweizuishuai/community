package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.TopicCreateDTO;
import com.buguagaoshu.community.dto.TopicSettingsDTO;
import com.buguagaoshu.community.model.User;

import java.util.Map;

/**
 * 用户创建话题、我的话题、吧主设置与管理员审核。
 */
public interface TopicService {

    Map<String, Object> applyTopic(TopicCreateDTO dto, User user);

    Map<String, Object> mine(User user);

    void updateSettings(long topicId, TopicSettingsDTO dto, User operator);

    void reviewTopic(long topicId, boolean approved, String reason, User admin);
}
