package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.TopicAccessDto;
import com.buguagaoshu.community.dto.TopicCreateDTO;
import com.buguagaoshu.community.dto.TopicSettingsDTO;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.TopicVisibilityEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.mapper.TopicMemberMapper;
import com.buguagaoshu.community.mapper.TopicModeratorMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.TopicMember;
import com.buguagaoshu.community.model.TopicModerator;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TopicAccessService;
import com.buguagaoshu.community.service.TopicService;
import com.buguagaoshu.community.util.CacheInvalidation;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.buguagaoshu.community.model.table.TagClassTableDef.TAG_CLASS;

/**
 * 话题创建与审核。用户提交的话题默认待审核且不公开展示，审核通过后创建者成为吧主。
 */
@Service
@Slf4j
public class TopicServiceImpl implements TopicService {

    private static final String DEFAULT_IMAGE = "/image/topicicon/default.svg";

    private final TagClassMapper tagClassMapper;
    private final TagClassCache tagClassCache;
    private final ClassificationCache classificationCache;
    private final TopicMemberMapper topicMemberMapper;
    private final TopicModeratorMapper topicModeratorMapper;
    private final TopicAccessService topicAccessService;
    private final NotificationService notificationService;
    private final SiteCacheService siteCache;

    public TopicServiceImpl(TagClassMapper tagClassMapper,
                            TagClassCache tagClassCache,
                            ClassificationCache classificationCache,
                            TopicMemberMapper topicMemberMapper,
                            TopicModeratorMapper topicModeratorMapper,
                            TopicAccessService topicAccessService,
                            NotificationService notificationService,
                            SiteCacheService siteCache) {
        this.tagClassMapper = tagClassMapper;
        this.tagClassCache = tagClassCache;
        this.classificationCache = classificationCache;
        this.topicMemberMapper = topicMemberMapper;
        this.topicModeratorMapper = topicModeratorMapper;
        this.topicAccessService = topicAccessService;
        this.notificationService = notificationService;
        this.siteCache = siteCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyTopic(TopicCreateDTO dto, User user) {
        String title = StringUtils.trimToEmpty(dto.getTitle());
        int visibility = TopicVisibilityEnum.of(dto.getVisibility()).getCode();
        if (dto.getType() == null || !classificationCache.isValidTopicCode(dto.getType())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "所选话题分类不存在或已停用");
        }
        TagClass sameTitle = tagClassMapper.selectOneByQuery(
                QueryWrapper.create().where(TAG_CLASS.TITLE.eq(title)));
        if (sameTitle != null) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "话题名称「" + title + "」已存在");
        }
        long now = System.currentTimeMillis();
        TagClass topic = new TagClass();
        topic.setTitle(title);
        topic.setType(dto.getType());
        topic.setSimpleDesc(StringUtils.trimToNull(dto.getSimpleDesc()));
        topic.setImage(StringUtils.isBlank(dto.getImage()) ? DEFAULT_IMAGE : dto.getImage().trim());
        topic.setVisibility(visibility);
        topic.setCreatedBy(user.getId());
        topic.setReviewStatus(0);
        topic.setStatus(0);
        topic.setTalkCount(0);
        topic.setFollowCount(0);
        topic.setCreateTime(now);
        topic.setModifiedTime(now);
        tagClassMapper.insertSelective(topic);
        invalidateTopicCaches();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("topicId", topic.getId());
        data.put("reviewStatus", topic.getReviewStatus());
        return data;
    }

    @Override
    public Map<String, Object> mine(User user) {
        List<TagClass> created = tagClassMapper.selectByCreator(user.getId());
        List<TopicMember> memberships = topicMemberMapper.selectByUser(user.getId());
        Set<Long> topicIds = memberships.stream().map(TopicMember::getTopicId).collect(Collectors.toSet());
        Map<Long, TagClass> topicMap = topicIds.isEmpty() ? Map.of()
                : tagClassMapper.selectListByIds(topicIds).stream()
                        .collect(Collectors.toMap(TagClass::getId, Function.identity()));
        List<Map<String, Object>> joined = new ArrayList<>();
        for (TopicMember member : memberships) {
            TagClass topic = topicMap.get(member.getTopicId());
            if (topic == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("memberId", member.getId());
            item.put("status", member.getStatus());
            item.put("joinType", member.getJoinType());
            item.put("message", member.getMessage());
            item.put("createTime", member.getCreateTime());
            item.put("topic", topic);
            joined.add(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("created", created);
        data.put("joined", joined);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(long topicId, TopicSettingsDTO dto, User operator) {
        TopicAccessDto access = topicAccessService.resolve(topicId, operator);
        if (access.topic() == null || !access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (!access.isOwner() && !access.isAdmin()) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        int visibility = dto.getVisibility() == null
                ? access.topic().getVisibility() : TopicVisibilityEnum.of(dto.getVisibility()).getCode();
        String image = StringUtils.isBlank(dto.getImage()) ? access.topic().getImage() : dto.getImage().trim();
        tagClassMapper.updateSettings(topicId, StringUtils.trimToNull(dto.getSimpleDesc()), image,
                visibility, System.currentTimeMillis());
        invalidateTopicCaches();
        log.info("用户 {} 修改了话题 {} 的设置，可见性 {}", operator.getId(), topicId, visibility);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewTopic(long topicId, boolean approved, String reason, User admin) {
        TagClass topic = tagClassMapper.selectOneById(topicId);
        if (topic == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (topic.getReviewStatus() != 0) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
        }
        String cleanReason = StringUtils.trimToNull(reason);
        if (!approved && cleanReason == null) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_REASON_REQUIRED);
        }
        long now = System.currentTimeMillis();
        int rows = tagClassMapper.reviewPending(topicId, approved ? 1 : 2, approved ? 1 : 0,
                approved ? null : cleanReason, admin.getId(), now);
        if (rows == 0) {
            throw new CustomizeException(CustomizeErrorCode.REVIEW_ALREADY_HANDLED);
        }
        if (approved && topic.getCreatedBy() != null) {
            ensureOwner(topicId, topic.getCreatedBy(), admin.getId());
        }
        if (topic.getCreatedBy() != null) {
            notificationService.createSystemNotification(topic.getCreatedBy(), admin.getId(), topicId, -1,
                    approved ? NotificationTypeEnum.TOPIC_REVIEW_PASS.getType()
                            : NotificationTypeEnum.TOPIC_REVIEW_REJECT.getType(),
                    approved
                            ? "你已成为吧主，可以完善话题简介、发布规则并管理成员了"
                            : "未通过原因：" + cleanReason);
        }
        invalidateTopicCaches();
        log.info("管理员 {} {} 了用户 {} 创建的话题 {}", admin.getId(),
                approved ? "通过" : "驳回", topic.getCreatedBy(), topicId);
    }

    /** 审核通过时幂等写入吧主；已有吧主时降级为小吧 */
    private void ensureOwner(long topicId, long userId, long operatorId) {
        topicModeratorMapper.demoteOwners(topicId);
        TopicModerator existing = topicModeratorMapper.selectOne(topicId, userId);
        if (existing != null) {
            existing.setRole(TopicModerator.ROLE_OWNER);
            topicModeratorMapper.update(existing);
            return;
        }
        TopicModerator owner = new TopicModerator();
        owner.setTopicId(topicId);
        owner.setUserId(userId);
        owner.setRole(TopicModerator.ROLE_OWNER);
        owner.setAssignedBy(operatorId);
        owner.setCreateTime(System.currentTimeMillis());
        topicModeratorMapper.insertSelective(owner);
    }

    private void invalidateTopicCaches() {
        CacheInvalidation.afterCommit(() -> {
            tagClassCache.refresh();
            siteCache.invalidate(SiteCacheService.TOPICS);
            siteCache.invalidate(SiteCacheService.TOPICS + "options");
            siteCache.invalidate(SiteCacheService.META + "tagClasses");
            siteCache.invalidate(SiteCacheService.HOME);
            siteCache.invalidate(SiteCacheService.META + "hots");
            siteCache.invalidate(SiteCacheService.META + "hotQuestions");
        });
    }
}
