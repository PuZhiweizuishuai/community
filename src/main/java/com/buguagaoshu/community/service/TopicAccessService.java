package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.model.User;

/**
 * 话题可见性与访问权限的统一入口。
 */
public interface TopicAccessService {

    /**
     * 解析当前访问者对话题的访问能力。话题不存在时返回 visible=false。
     */
    TopicAccessDto resolve(long topicId, User viewer);

    /**
     * 要求可查看话题内容：不可见按 404 处理，私有可申请但非成员按 403 处理。
     */
    void requireContentAccess(long topicId, User viewer);

    /**
     * 是否可以在话题内发帖：公开话题所有登录用户可发，私有话题仅成员/吧务/创建者/管理员可发。
     */
    boolean canPostInTopic(long topicId, User user);
}
