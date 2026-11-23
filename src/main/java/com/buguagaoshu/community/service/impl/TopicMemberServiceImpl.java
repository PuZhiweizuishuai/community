package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.dto.TopicMemberDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
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
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.TopicAccessService;
import com.buguagaoshu.community.service.TopicMemberService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 话题成员关系实现。申请与邀请共用一个唯一键，状态流转统一走 CAS。
 */
@Service
public class TopicMemberServiceImpl implements TopicMemberService {

    private final TopicMemberMapper topicMemberMapper;
    private final TopicModeratorMapper topicModeratorMapper;
    private final TopicAccessService topicAccessService;
    private final UserService userService;
    private final UserBatchLoader userBatchLoader;
    private final NotificationService notificationService;

    public TopicMemberServiceImpl(TopicMemberMapper topicMemberMapper,
                                  TopicModeratorMapper topicModeratorMapper,
                                  TopicAccessService topicAccessService,
                                  UserService userService,
                                  UserBatchLoader userBatchLoader,
                                  NotificationService notificationService) {
        this.topicMemberMapper = topicMemberMapper;
        this.topicModeratorMapper = topicModeratorMapper;
        this.topicAccessService = topicAccessService;
        this.userService = userService;
        this.userBatchLoader = userBatchLoader;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(long topicId, String message, User user) {
        TopicAccessDto access = topicAccessService.resolve(topicId, user);
        TagClass topic = access.topic();
        if (topic == null || !access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (topic.getReviewStatus() != 1 || topic.getStatus() != 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "话题尚未通过审核");
        }
        if (topic.getVisibility() == TopicVisibilityEnum.PUBLIC.getCode()) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "公开话题无需申请加入");
        }
        if (topic.getVisibility() == TopicVisibilityEnum.PRIVATE_HIDDEN.getCode()) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(), "该话题仅限邀请加入");
        }
        long now = System.currentTimeMillis();
        String cleanMessage = StringUtils.trimToNull(StringUtils.substring(message, 0, 200));
        TopicMember existing = topicMemberMapper.selectOne(topicId, user.getId());
        if (existing != null) {
            if (existing.getStatus() == TopicMemberStatusEnum.APPROVED.getCode()) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "你已经是该话题成员");
            }
            if (existing.getStatus() == TopicMemberStatusEnum.PENDING.getCode()) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(),
                        TopicJoinTypeEnum.INVITE.getCode().equals(existing.getJoinType())
                                ? "你已被邀请，请直接在话题页接受邀请" : "申请正在审核中，请耐心等待");
            }
            if (topicMemberMapper.reopenApply(existing.getId(), topicId, user.getId(), cleanMessage, now) == 0) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT);
            }
        } else {
            TopicMember created = new TopicMember();
            created.setTopicId(topicId);
            created.setUserId(user.getId());
            created.setStatus(TopicMemberStatusEnum.PENDING.getCode());
            created.setJoinType(TopicJoinTypeEnum.APPLY.getCode());
            created.setMessage(cleanMessage);
            created.setCreateTime(now);
            try {
                topicMemberMapper.insertSelective(created);
            } catch (DuplicateKeyException e) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "申请已提交，请勿重复操作");
            }
        }
        notifyOwners(topicId, user, topic);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invite(long topicId, String userCode, User operator) {
        requireManage(topicId, operator);
        if (StringUtils.isBlank(userCode)) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        User target = userService.selectUserByUserId(userCode.trim());
        if (target == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "用户不存在，请确认登录账号");
        }
        if (target.getId() == operator.getId()) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "不能邀请自己");
        }
        TopicModerator moderator = topicModeratorMapper.selectOne(topicId, target.getId());
        if (moderator != null) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "该用户已是话题吧务");
        }
        long now = System.currentTimeMillis();
        TopicMember existing = topicMemberMapper.selectOne(topicId, target.getId());
        if (existing != null) {
            if (existing.getStatus() == TopicMemberStatusEnum.APPROVED.getCode()) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "该用户已经是话题成员");
            }
            if (existing.getStatus() == TopicMemberStatusEnum.PENDING.getCode()) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "该用户已有待处理的申请或邀请");
            }
            if (topicMemberMapper.reopenInvite(existing.getId(), topicId, target.getId(), operator.getId(), now) == 0) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT);
            }
        } else {
            TopicMember created = new TopicMember();
            created.setTopicId(topicId);
            created.setUserId(target.getId());
            created.setStatus(TopicMemberStatusEnum.PENDING.getCode());
            created.setJoinType(TopicJoinTypeEnum.INVITE.getCode());
            created.setReviewerId(operator.getId());
            created.setCreateTime(now);
            try {
                topicMemberMapper.insertSelective(created);
            } catch (DuplicateKeyException e) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "邀请已发送，请勿重复操作");
            }
        }
        TagClass topic = topicAccessService.resolve(topicId, operator).topic();
        notificationService.createSystemNotification(target.getId(), operator.getId(), topicId, -1,
                NotificationTypeEnum.TOPIC_INVITE.getType(),
                "吧主邀请你加入话题「" + (topic == null ? "" : topic.getTitle()) + "」，请前往话题页接受邀请");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void answerInvite(long topicId, User user, boolean accept) {
        TopicMember member = topicMemberMapper.selectOne(topicId, user.getId());
        if (member == null || member.getStatus() != TopicMemberStatusEnum.PENDING.getCode()
                || !TopicJoinTypeEnum.INVITE.getCode().equals(member.getJoinType())) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "没有待处理的邀请");
        }
        int status = accept ? TopicMemberStatusEnum.APPROVED.getCode() : TopicMemberStatusEnum.REJECTED.getCode();
        if (topicMemberMapper.answerInvite(member.getId(), topicId, user.getId(), status,
                user.getId(), System.currentTimeMillis()) == 0) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT);
        }
    }

    @Override
    public PaginationDto<TopicMemberDto> listMembers(long topicId, Integer status, String joinType,
                                                     String page, String size, User operator) {
        requireManage(topicId, operator);
        String cleanJoinType = StringUtils.trimToNull(joinType);
        long total = topicMemberMapper.countByTopic(topicId, status, cleanJoinType);
        long[] param = NumberUtils.getPageAndSize(page, size, total);
        List<TopicMember> members = topicMemberMapper.pageByTopic(topicId, status, cleanJoinType,
                param[0], param[1]);
        Map<Long, User> userMap = userBatchLoader.loadUsers(
                members.stream().map(TopicMember::getUserId).collect(Collectors.toSet()));
        List<TopicMemberDto> data = members.stream().map(member -> toDto(member, userMap)).toList();
        PaginationDto<TopicMemberDto> pagination = new PaginationDto<>();
        pagination.setData(data);
        pagination.setPagination(param[2], param[3], param[1]);
        pagination.setAllCount(total);
        return pagination;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(long topicId, long memberId, boolean approve, String reason, User operator) {
        requireManage(topicId, operator);
        TopicMember member = topicMemberMapper.selectOneById(memberId);
        if (member == null || member.getTopicId() != topicId
                || member.getStatus() != TopicMemberStatusEnum.PENDING.getCode()
                || !TopicJoinTypeEnum.APPLY.getCode().equals(member.getJoinType())) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
        }
        int status = approve ? TopicMemberStatusEnum.APPROVED.getCode() : TopicMemberStatusEnum.REJECTED.getCode();
        if (topicMemberMapper.updateStatusCas(memberId, topicId, status,
                TopicMemberStatusEnum.PENDING.getCode(), operator.getId(), System.currentTimeMillis()) == 0) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
        }
        TagClass topic = topicAccessService.resolve(topicId, operator).topic();
        String resultText = approve ? "已通过"
                : "未通过" + (StringUtils.isBlank(reason) ? "" : "：" + reason);
        String content = "你加入话题「" + (topic == null ? "" : topic.getTitle()) + "」的申请" + resultText;
        notificationService.createSystemNotification(member.getUserId(), operator.getId(), topicId, -1,
                approve ? NotificationTypeEnum.TOPIC_APPLY_PASS.getType()
                        : NotificationTypeEnum.TOPIC_APPLY_REJECT.getType(),
                content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(long topicId, long userId, User operator) {
        TopicAccessDto access = topicAccessService.resolve(topicId, operator);
        if (access.topic() == null || !access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (!access.isOwner() && !access.isAdmin()) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        if (topicModeratorMapper.selectOne(topicId, userId) != null) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "请先卸任该用户的吧务身份");
        }
        topicMemberMapper.deleteByTopicAndUser(topicId, userId);
    }

    private void requireManage(long topicId, User operator) {
        TopicAccessDto access = topicAccessService.resolve(topicId, operator);
        if (access.topic() == null || !access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (!access.isOwner() && !access.isModerator() && !access.isAdmin()) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
    }

    /** 申请加入时通知整个吧务团队（吧主与小吧均可审核） */
    private void notifyOwners(long topicId, User applicant, TagClass topic) {
        List<TopicModerator> moderators = topicModeratorMapper.selectByTopicId(topicId);
        Set<Long> receiverIds = moderators.stream().map(TopicModerator::getUserId).collect(Collectors.toSet());
        for (Long receiverId : receiverIds) {
            if (receiverId == null || receiverId == applicant.getId()) {
                continue;
            }
            notificationService.createSystemNotification(receiverId, applicant.getId(), topicId, -1,
                    NotificationTypeEnum.TOPIC_APPLY_RECEIVED.getType(),
                    "用户「" + applicant.getUserName() + "」申请加入话题「"
                            + (topic == null ? "" : topic.getTitle()) + "」，请前往话题页审核");
        }
    }

    private TopicMemberDto toDto(TopicMember member, Map<Long, User> userMap) {
        TopicMemberDto dto = new TopicMemberDto();
        dto.setId(member.getId());
        dto.setTopicId(member.getTopicId());
        dto.setUserId(member.getUserId());
        dto.setStatus(member.getStatus());
        dto.setJoinType(member.getJoinType());
        dto.setMessage(member.getMessage());
        dto.setCreateTime(member.getCreateTime());
        User user = userMap.get(member.getUserId());
        if (user != null) {
            dto.setUserCode(user.getUserId());
            dto.setUserName(user.getUserName());
            dto.setHeadUrl(user.getHeadUrl());
        }
        return dto;
    }
}
