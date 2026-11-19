package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.TagAdminDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionTagMapper;
import com.buguagaoshu.community.mapper.TagMapper;
import com.buguagaoshu.community.model.Tag;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionTagService;
import com.buguagaoshu.community.service.SiteCacheService;
import com.buguagaoshu.community.service.TagAdminService;
import com.buguagaoshu.community.util.CacheInvalidation;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.buguagaoshu.community.model.table.TagTableDef.TAG;

/**
 * 后台标签管理实现。
 *
 * <p>标签审核状态变更会影响所有关联帖子的公开展示，因此每次变更都要刷新
 * questions.tag 冗余列并失效相关页面缓存。
 */
@Service
@Slf4j
public class TagAdminServiceImpl implements TagAdminService {

    private static final int MAX_NAME_LENGTH = 50;

    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;
    private final QuestionTagService questionTagService;
    private final NotificationService notificationService;
    private final UserBatchLoader userBatchLoader;
    private final SiteCacheService siteCache;

    public TagAdminServiceImpl(TagMapper tagMapper, QuestionTagMapper questionTagMapper,
                               QuestionTagService questionTagService,
                               NotificationService notificationService,
                               UserBatchLoader userBatchLoader,
                               SiteCacheService siteCache) {
        this.tagMapper = tagMapper;
        this.questionTagMapper = questionTagMapper;
        this.questionTagService = questionTagService;
        this.notificationService = notificationService;
        this.userBatchLoader = userBatchLoader;
        this.siteCache = siteCache;
    }

    @Override
    public PaginationDto<TagAdminDto> list(Integer status, String keyword, String page, String size) {
        String kw = StringUtils.trimToNull(keyword);
        QueryWrapper condition = QueryWrapper.create();
        if (status != null) {
            condition.and(TAG.STATUS.eq(status));
        }
        if (kw != null) {
            condition.and(TAG.NAME.like(kw));
        }
        long total = tagMapper.selectCountByQuery(condition);
        long[] param = NumberUtils.getPageAndSize(page, size, total);
        List<Tag> tags = tagMapper.selectListByQuery(condition
                .orderBy(TAG.STATUS.asc(), TAG.USAGE_COUNT.desc(), TAG.ID.desc())
                .limit(param[0], param[1]));

        Set<Long> userIds = tags.stream()
                .flatMap(tag -> java.util.stream.Stream.of(tag.getCreatedBy(), tag.getReviewerId()))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userBatchLoader.loadUsers(userIds);

        List<TagAdminDto> rows = tags.stream().map(tag -> {
            TagAdminDto dto = new TagAdminDto();
            dto.setId(tag.getId());
            dto.setName(tag.getName());
            dto.setStatus(tag.getStatus());
            dto.setUsageCount(tag.getUsageCount());
            dto.setCreatedBy(tag.getCreatedBy());
            dto.setCreateTime(tag.getCreateTime());
            dto.setReviewerId(tag.getReviewerId());
            dto.setReviewTime(tag.getReviewTime());
            dto.setRejectReason(tag.getRejectReason());
            User creator = tag.getCreatedBy() == null ? null : userMap.get(tag.getCreatedBy());
            User reviewer = tag.getReviewerId() == null ? null : userMap.get(tag.getReviewerId());
            dto.setCreatorName(creator == null ? null : creator.getUserName());
            dto.setReviewerName(reviewer == null ? null : reviewer.getUserName());
            return dto;
        }).toList();

        PaginationDto<TagAdminDto> result = new PaginationDto<>();
        result.setData(rows);
        result.setPagination(param[2], param[3], param[1]);
        result.setAllCount(total);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag approve(long id, User admin) {
        Tag tag = requireTag(id);
        if (tag.getStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
            return tag;
        }
        long now = System.currentTimeMillis();
        tagMapper.updateStatus(id, ReviewStatusEnum.APPROVED.getStatus(), admin.getId(), now, null);
        refreshQuestionsOfTag(id);
        notifyCreator(tag, admin, NotificationTypeEnum.TAG_REVIEW_PASS,
                "你创建的标签「" + tag.getName() + "」已通过审核，所有关联帖子现在会公开显示该标签");
        invalidateCaches();
        log.info("管理员 {} 通过标签 {}（{}）", admin.getId(), id, tag.getName());
        tag.setStatus(ReviewStatusEnum.APPROVED.getStatus());
        tag.setReviewerId(admin.getId());
        tag.setReviewTime(now);
        tag.setRejectReason(null);
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag reject(long id, String reason, User admin) {
        Tag tag = requireTag(id);
        String cleanReason = StringUtils.trimToNull(reason);
        if (cleanReason == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写不通过原因");
        }
        if (cleanReason.length() > 200) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "不通过原因不能超过 200 个字符");
        }
        long now = System.currentTimeMillis();
        tagMapper.updateStatus(id, ReviewStatusEnum.REJECTED.getStatus(), admin.getId(),
                now, cleanReason);
        refreshQuestionsOfTag(id);
        notifyCreator(tag, admin, NotificationTypeEnum.TAG_REVIEW_REJECT,
                "你创建的标签「" + tag.getName() + "」未通过审核：" + cleanReason);
        invalidateCaches();
        log.info("管理员 {} 拒绝标签 {}（{}）：{}", admin.getId(), id, tag.getName(), cleanReason);
        tag.setStatus(ReviewStatusEnum.REJECTED.getStatus());
        tag.setReviewerId(admin.getId());
        tag.setReviewTime(now);
        tag.setRejectReason(cleanReason);
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag rename(long id, String name, User admin) {
        Tag tag = requireTag(id);
        String cleanName = StringUtils.trimToNull(name);
        if (cleanName == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "标签名称不能为空");
        }
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "标签名称不能超过 50 个字符");
        }
        Tag same = tagMapper.selectByName(cleanName);
        if (same != null && same.getId() != id) {
            throw new CustomizeException(409, "已存在同名标签「" + cleanName + "」");
        }
        try {
            tagMapper.updateName(id, cleanName);
        } catch (DuplicateKeyException e) {
            // 并发重命名撞唯一键，按同名冲突处理
            throw new CustomizeException(409, "已存在同名标签「" + cleanName + "」");
        }
        refreshQuestionsOfTag(id);
        invalidateCaches();
        log.info("管理员 {} 将标签 {} 重命名为 {}", admin.getId(), id, cleanName);
        tag.setName(cleanName);
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id, User admin) {
        Tag tag = requireTag(id);
        // 先记住受影响的帖子，解除关联后再刷新冗余列，否则被删标签仍会留在 questions.tag
        Set<Long> questionIds = new java.util.HashSet<>(questionTagMapper.selectQuestionIdsByTagId(id));
        questionTagMapper.deleteByTagId(id);
        tagMapper.deleteById(id);
        questionTagService.refreshTagNames(questionIds);
        invalidateCaches();
        log.info("管理员 {} 删除标签 {}（{}）", admin.getId(), id, tag.getName());
    }

    private Tag requireTag(long id) {
        Tag tag = id <= 0 ? null : tagMapper.selectOneById(id);
        if (tag == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        return tag;
    }

    /** 标签审核状态/名称变化后，重算所有关联帖子的冗余标签列 */
    private void refreshQuestionsOfTag(long tagId) {
        Set<Long> questionIds = new java.util.HashSet<>(questionTagMapper.selectQuestionIdsByTagId(tagId));
        questionTagService.refreshTagNames(questionIds);
    }

    private void notifyCreator(Tag tag, User admin, NotificationTypeEnum type, String content) {
        if (tag.getCreatedBy() == null || tag.getCreatedBy() <= 0) {
            return;
        }
        notificationService.createSystemNotification(tag.getCreatedBy(), admin.getId(), 0L, -1,
                type.getType(), content);
    }

    private void invalidateCaches() {
        CacheInvalidation.afterCommit(() -> {
            siteCache.invalidate(SiteCacheService.HOME);
            siteCache.invalidate(SiteCacheService.META + "hots");
            siteCache.invalidate(SiteCacheService.META + "tagCategories");
            siteCache.invalidate(SiteCacheService.META + "approvedTags");
        });
    }
}
