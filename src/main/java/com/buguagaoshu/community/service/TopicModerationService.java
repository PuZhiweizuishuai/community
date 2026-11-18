package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.TopicModeratorDto;
import com.buguagaoshu.community.model.TopicModerator;
import com.buguagaoshu.community.model.User;

import java.util.List;

/**
 * 话题吧务：成员关系本身即资源范围权限，所有操作都要双重校验
 * （当前用户是该话题吧务 + 目标帖子属于该话题）。
 */
public interface TopicModerationService {

    /** 是否具备该话题的吧务权限（全局问题管理员也视为具备） */
    boolean isModerator(User user, long topicId);

    /** 无吧务权限时抛出 403 */
    void requireModerator(User user, long topicId);

    /** 话题吧务团队（含吧主/小吧） */
    List<TopicModeratorDto> moderators(long topicId);

    /** 后台任命（已有记录则更新角色）；userCode 为用户登录账号 */
    TopicModerator assign(long topicId, String userCode, String role, User admin);

    /** 后台卸任 */
    void remove(long topicId, long userId, User admin);

    /** 吧务删除本话题帖子 */
    void deleteQuestion(long topicId, long questionId, User operator);

    /** 吧务恢复本话题帖子 */
    void recoverQuestion(long topicId, long questionId, User operator);

    /** 吧务置顶/取消置顶 */
    void setTop(long topicId, long questionId, boolean top, User operator);

    /** 吧务加精/取消加精 */
    void setEssence(long topicId, long questionId, boolean essence, User operator);

    /** 吧务修改标签 */
    void updateTags(long topicId, long questionId, List<String> tagNames, User operator);
}
