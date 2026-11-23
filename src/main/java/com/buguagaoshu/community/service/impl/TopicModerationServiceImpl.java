package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.TopicModeratorDto;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.TopicModeratorMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.TopicModerator;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.QuestionTagService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.service.TopicModerationService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.CacheInvalidation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 吧务实现：删除/恢复复用 QuestionService，置顶/加精/改标签直接操作帖子表。
 */
@Service
@Slf4j
public class TopicModerationServiceImpl implements TopicModerationService {

    private final TopicModeratorMapper moderatorMapper;
    private final QuestionMapper questionMapper;
    private final QuestionService questionService;
    private final QuestionTagService questionTagService;
    private final TagClassService tagClassService;
    private final UserService userService;
    private final UserBatchLoader userBatchLoader;
    private final SiteCacheService siteCache;

    public TopicModerationServiceImpl(TopicModeratorMapper moderatorMapper,
                                      QuestionMapper questionMapper,
                                      QuestionService questionService,
                                      QuestionTagService questionTagService,
                                      TagClassService tagClassService,
                                      UserService userService,
                                      UserBatchLoader userBatchLoader,
                                      SiteCacheService siteCache) {
        this.moderatorMapper = moderatorMapper;
        this.questionMapper = questionMapper;
        this.questionService = questionService;
        this.questionTagService = questionTagService;
        this.tagClassService = tagClassService;
        this.userService = userService;
        this.userBatchLoader = userBatchLoader;
        this.siteCache = siteCache;
    }

    @Override
    public boolean isModerator(User user, long topicId) {
        if (user == null || topicId <= 0) {
            return false;
        }
        // 全局管理员不受话题限制
        if (AuthUtil.hasPermission(user, PermissionCodes.ADMIN_QUESTION)
                || AuthUtil.hasPermission(user, PermissionCodes.ADMIN_TOPIC)
                || AuthUtil.hasPermission(user, PermissionCodes.ADMIN_ACCESS)) {
            return true;
        }
        return moderatorMapper.selectOne(topicId, user.getId()) != null;
    }

    @Override
    public void requireModerator(User user, long topicId) {
        if (!isModerator(user, topicId)) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
    }

    @Override
    public List<TopicModeratorDto> moderators(long topicId) {
        List<TopicModerator> moderators = moderatorMapper.selectByTopicId(topicId);
        if (moderators.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = moderators.stream().map(TopicModerator::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userBatchLoader.loadUsers(userIds);
        return moderators.stream().map(item -> {
            TopicModeratorDto dto = new TopicModeratorDto();
            dto.setId(item.getId());
            dto.setUserId(item.getUserId());
            dto.setRole(item.getRole());
            dto.setCreateTime(item.getCreateTime());
            User user = userMap.get(item.getUserId());
            if (user != null) {
                dto.setUserCode(user.getUserId());
                dto.setUserName(user.getUserName());
                dto.setHeadUrl(user.getHeadUrl());
            }
            return dto;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicModerator assign(long topicId, String userCode, String role, User admin) {
        TagClass topic = tagClassService.findById(topicId);
        if (topic == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (userCode == null || userCode.isBlank()) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        User user = userService.selectUserByUserId(userCode.trim());
        if (user == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "用户不存在，请确认登录账号");
        }
        long userId = user.getId();
        String cleanRole = TopicModerator.ROLE_OWNER.equals(role)
                ? TopicModerator.ROLE_OWNER : TopicModerator.ROLE_MODERATOR;
        if (TopicModerator.ROLE_OWNER.equals(cleanRole)) {
            // 每个话题只保留一个吧主，新吧主上任时旧吧主自动降级为小吧
            moderatorMapper.demoteOwners(topicId);
        }
        TopicModerator exists = moderatorMapper.selectOne(topicId, userId);
        if (exists != null) {
            exists.setRole(cleanRole);
            moderatorMapper.update(exists);
            return exists;
        }
        TopicModerator created = new TopicModerator();
        created.setTopicId(topicId);
        created.setUserId(userId);
        created.setRole(cleanRole);
        created.setAssignedBy(admin.getId());
        created.setCreateTime(System.currentTimeMillis());
        try {
            moderatorMapper.insertSelective(created);
        } catch (DuplicateKeyException e) {
            TopicModerator again = moderatorMapper.selectOne(topicId, userId);
            if (again != null) {
                again.setRole(cleanRole);
                moderatorMapper.update(again);
                return again;
            }
        }
        log.info("管理员 {} 任命用户 {} 为话题 {} 的{}", admin.getId(), userId, topicId,
                TopicModerator.ROLE_OWNER.equals(cleanRole) ? "吧主" : "小吧");
        return created;
    }

    @Override
    public void remove(long topicId, long userId, User admin) {
        TopicModerator target = moderatorMapper.selectOne(topicId, userId);
        if (target != null && TopicModerator.ROLE_OWNER.equals(target.getRole())
                && moderatorMapper.selectOwners(topicId).size() <= 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(),
                    "该用户是话题唯一吧主，请先任命新的吧主");
        }
        if (moderatorMapper.deleteByTopicAndUser(topicId, userId) > 0) {
            log.info("管理员 {} 卸任话题 {} 的吧务 {}", admin.getId(), topicId, userId);
        }
    }

    @Override
    public void deleteQuestion(long topicId, long questionId, User operator) {
        Question question = requireQuestionOfTopic(topicId, questionId, operator);
        questionService.deleteQuestion(question.getQuestionId(), operator, true);
        invalidateCaches();
    }

    @Override
    public void recoverQuestion(long topicId, long questionId, User operator) {
        Question question = requireQuestionOfTopic(topicId, questionId, operator);
        questionService.recoveryQuestion(question.getQuestionId(), operator);
        invalidateCaches();
    }

    @Override
    public void setTop(long topicId, long questionId, boolean top, User operator) {
        requireQuestionOfTopic(topicId, questionId, operator);
        questionMapper.updateTop(questionId, top ? 1 : 0);
        invalidateCaches();
        log.info("吧务 {} 把问题 {} 的置顶状态改为 {}", operator.getId(), questionId, top);
    }

    @Override
    public void setEssence(long topicId, long questionId, boolean essence, User operator) {
        requireQuestionOfTopic(topicId, questionId, operator);
        questionMapper.updateEssence(questionId, essence ? 1 : 0);
        invalidateCaches();
        log.info("吧务 {} 把问题 {} 的加精状态改为 {}", operator.getId(), questionId, essence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTags(long topicId, long questionId, List<String> tagNames, User operator) {
        requireQuestionOfTopic(topicId, questionId, operator);
        if (tagNames != null && tagNames.size() > 5) {
            throw new CustomizeException(CustomizeErrorCode.TAG_LIMIT);
        }
        questionTagService.saveQuestionTags(questionId, tagNames, operator.getId());
        invalidateCaches();
        log.info("吧务 {} 修改了问题 {} 的标签", operator.getId(), questionId);
    }

    /** 双重校验：吧务身份 + 目标帖子归属本话题 */
    private Question requireQuestionOfTopic(long topicId, long questionId, User operator) {
        requireModerator(operator, topicId);
        Question question = questionMapper.selectOneById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        if (question.getTopicId() == null || question.getTopicId() != topicId) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        return question;
    }

    private void invalidateCaches() {
        CacheInvalidation.afterCommit(() -> {
            siteCache.invalidate(SiteCacheService.HOME);
            siteCache.invalidate(SiteCacheService.TOPICS);
        });
    }
}
