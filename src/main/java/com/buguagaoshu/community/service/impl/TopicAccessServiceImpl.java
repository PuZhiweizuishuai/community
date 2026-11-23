package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.enums.TopicJoinTypeEnum;
import com.buguagaoshu.community.enums.TopicMemberStatusEnum;
import com.buguagaoshu.community.enums.TopicVisibilityEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.TopicMemberMapper;
import com.buguagaoshu.community.mapper.TopicModeratorMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.TopicMember;
import com.buguagaoshu.community.model.TopicModerator;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.TopicAccessService;
import com.buguagaoshu.community.util.AuthUtil;
import org.springframework.stereotype.Service;

/**
 * 话题访问控制：可见性、成员关系、吧务身份与管理员绕过统一在这里判定。
 */
@Service
public class TopicAccessServiceImpl implements TopicAccessService {

    private final TagClassCache tagClassCache;
    private final TopicMemberMapper topicMemberMapper;
    private final TopicModeratorMapper topicModeratorMapper;

    public TopicAccessServiceImpl(TagClassCache tagClassCache,
                                  TopicMemberMapper topicMemberMapper,
                                  TopicModeratorMapper topicModeratorMapper) {
        this.tagClassCache = tagClassCache;
        this.topicMemberMapper = topicMemberMapper;
        this.topicModeratorMapper = topicModeratorMapper;
    }

    @Override
    public TopicAccessDto resolve(long topicId, User viewer) {
        TagClass topic = topicId > 0 ? tagClassCache.getById(topicId) : null;
        if (topic == null) {
            return new TopicAccessDto(null, false, false, false,
                    false, false, false, false, "none", null);
        }
        Long userId = viewer == null ? null : viewer.getId();
        boolean admin = isAdmin(viewer);
        boolean creator = userId != null && topic.getCreatedBy() != null
                && topic.getCreatedBy().equals(userId);

        TopicModerator moderator = userId == null ? null : topicModeratorMapper.selectOne(topicId, userId);
        boolean owner = moderator != null && TopicModerator.ROLE_OWNER.equals(moderator.getRole());
        boolean isModerator = moderator != null && !owner;

        TopicMember member = userId == null ? null : topicMemberMapper.selectOne(topicId, userId);
        boolean joined = member != null && member.getStatus() == TopicMemberStatusEnum.APPROVED.getCode();

        boolean approved = topic.getReviewStatus() == 1 && topic.getStatus() == 1;
        boolean contentRole = joined || owner || isModerator || creator || admin;

        String joinStatus = joinStatus(topic, member, joined, owner, isModerator, creator);
        String joinType = member == null ? null : member.getJoinType();

        boolean canViewInfo;
        boolean canViewContent;
        if (!approved) {
            // 待审核 / 未通过：仅创建者与管理员可见
            canViewInfo = creator || admin;
            canViewContent = canViewInfo;
        } else if (topic.getVisibility() == TopicVisibilityEnum.PUBLIC.getCode()) {
            canViewInfo = true;
            canViewContent = true;
        } else if (topic.getVisibility() == TopicVisibilityEnum.PRIVATE_APPLY.getCode()) {
            // 私有可申请：话题信息可见，内容仅成员可见
            canViewInfo = true;
            canViewContent = contentRole;
        } else {
            // 私有隐藏：只有成员 / 吧务 / 创建者 / 管理员，以及待接受邀请者可看到话题信息
            boolean invited = member != null
                    && member.getStatus() == TopicMemberStatusEnum.PENDING.getCode()
                    && TopicJoinTypeEnum.INVITE.getCode().equals(member.getJoinType());
            canViewContent = contentRole;
            canViewInfo = contentRole || invited;
        }
        return new TopicAccessDto(topic, canViewInfo, canViewInfo, canViewContent,
                joined, owner, isModerator, admin, joinStatus, joinType);
    }

    @Override
    public void requireContentAccess(long topicId, User viewer) {
        TopicAccessDto access = resolve(topicId, viewer);
        if (access.topic() == null || !access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (!access.canViewContent()) {
            // 私有话题内容对非成员按不存在处理，避免泄露成员限制
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
    }

    @Override
    public boolean canPostInTopic(long topicId, User user) {
        if (user == null) {
            return false;
        }
        TopicAccessDto access = resolve(topicId, user);
        if (access.topic() == null || !access.canViewContent()) {
            return false;
        }
        return access.topic().getReviewStatus() == 1 && access.topic().getStatus() == 1;
    }

    private String joinStatus(TagClass topic, TopicMember member, boolean joined,
                              boolean owner, boolean isModerator, boolean creator) {
        if (owner || isModerator || creator) {
            return "joined";
        }
        if (topic.getVisibility() == TopicVisibilityEnum.PUBLIC.getCode()
                && topic.getReviewStatus() == 1 && topic.getStatus() == 1) {
            return "public";
        }
        if (joined) {
            return "joined";
        }
        if (member != null) {
            if (member.getStatus() == TopicMemberStatusEnum.PENDING.getCode()) {
                return TopicJoinTypeEnum.INVITE.getCode().equals(member.getJoinType())
                        ? "invited" : "pending";
            }
            if (member.getStatus() == TopicMemberStatusEnum.REJECTED.getCode()) {
                return "rejected";
            }
        }
        return "none";
    }

    private boolean isAdmin(User viewer) {
        return viewer != null
                && (AuthUtil.hasPermission(viewer, PermissionCodes.ADMIN_TOPIC)
                || AuthUtil.hasPermission(viewer, PermissionCodes.ADMIN_QUESTION)
                || AuthUtil.hasPermission(viewer, PermissionCodes.ADMIN_ACCESS));
    }
}
