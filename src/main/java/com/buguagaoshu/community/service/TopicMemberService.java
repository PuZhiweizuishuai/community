package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.TopicMemberDto;
import com.buguagaoshu.community.model.User;

/**
 * 话题成员：申请、邀请、审核与移除。
 */
public interface TopicMemberService {

    /** 申请加入私有可申请话题 */
    void apply(long topicId, String message, User user);

    /** 吧务 / 吧主 / 管理员邀请用户 */
    void invite(long topicId, String userCode, User operator);

    /** 被邀请人接受或拒绝邀请 */
    void answerInvite(long topicId, User user, boolean accept);

    /** 吧务 / 吧主 / 管理员查看成员与待审申请；joinType 可选 apply / invite */
    PaginationDto<TopicMemberDto> listMembers(long topicId, Integer status, String joinType,
                                              String page, String size, User operator);

    /** 吧务 / 吧主 / 管理员审核加入申请 */
    void reviewApplication(long topicId, long memberId, boolean approve, String reason, User operator);

    /** 吧主 / 管理员移除成员或取消邀请 */
    void removeMember(long topicId, long userId, User operator);
}
