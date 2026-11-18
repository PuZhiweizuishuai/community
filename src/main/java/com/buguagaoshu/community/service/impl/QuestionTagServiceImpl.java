package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.QuestionTagDto;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.QuestionTagMapper;
import com.buguagaoshu.community.mapper.TagMapper;
import com.buguagaoshu.community.model.QuestionTag;
import com.buguagaoshu.community.model.Tag;
import com.buguagaoshu.community.service.QuestionTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标签服务实现。标签审核状态复用 ReviewStatusEnum 语义：
 * 0 待审核（不公开）、1 已通过（公开）、2 未通过（不公开，可重新提交）。
 */
@Service
@Slf4j
public class QuestionTagServiceImpl implements QuestionTagService {

    /** 与前端发布页上限保持一致 */
    public static final int MAX_TAGS = 5;
    public static final int MAX_TAG_NAME_LENGTH = 50;

    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;
    private final QuestionMapper questionMapper;

    public QuestionTagServiceImpl(TagMapper tagMapper, QuestionTagMapper questionTagMapper,
                                  QuestionMapper questionMapper) {
        this.tagMapper = tagMapper;
        this.questionTagMapper = questionTagMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    @Transactional
    public List<Tag> resolveTags(List<String> names, long userId) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        // 去重（忽略大小写）并过滤非法名称，最多 5 个；保留用户输入的首个展示形态
        Map<String, String> displayNames = new LinkedHashMap<>();
        for (String raw : names) {
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty() || name.length() > MAX_TAG_NAME_LENGTH) {
                continue;
            }
            String key = name.toLowerCase(Locale.ROOT);
            displayNames.putIfAbsent(key, name);
            if (displayNames.size() >= MAX_TAGS) {
                break;
            }
        }
        if (displayNames.isEmpty()) {
            return List.of();
        }

        long now = System.currentTimeMillis();
        List<Tag> result = new ArrayList<>(displayNames.size());
        for (Map.Entry<String, String> entry : displayNames.entrySet()) {
            String key = entry.getKey();
            Tag tag = tagMapper.selectByName(key);
            if (tag == null) {
                Tag created = new Tag();
                created.setName(entry.getValue());
                created.setStatus(ReviewStatusEnum.PENDING.getStatus());
                created.setUsageCount(0);
                created.setCreatedBy(userId > 0 ? userId : null);
                created.setCreateTime(now);
                try {
                    tagMapper.insertSelective(created);
                    tag = created;
                } catch (DuplicateKeyException e) {
                    // 并发创建同名标签：唯一键兜底，重新读取
                    tag = tagMapper.selectByName(key);
                }
            } else if (tag.getStatus() == ReviewStatusEnum.REJECTED.getStatus()) {
                // 被拒绝的标签允许重新提交，重置为待审核并清空审核结论
                tagMapper.updateStatus(tag.getId(), ReviewStatusEnum.PENDING.getStatus(), 0L, now, null);
                tag.setStatus(ReviewStatusEnum.PENDING.getStatus());
                tag.setReviewerId(null);
                tag.setReviewTime(null);
                tag.setRejectReason(null);
            }
            if (tag != null) {
                result.add(tag);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void replaceQuestionTags(long questionId, List<Tag> tags) {
        List<Long> oldTagIds = questionTagMapper.selectByQuestionId(questionId).stream()
                .map(QuestionTag::getTagId).toList();
        questionTagMapper.deleteByQuestionId(questionId);
        if (tags != null && !tags.isEmpty()) {
            List<QuestionTag> rows = new ArrayList<>(tags.size());
            for (Tag tag : tags) {
                QuestionTag row = new QuestionTag();
                row.setQuestionId(questionId);
                row.setTagId(tag.getId());
                rows.add(row);
            }
            questionTagMapper.insertBatch(rows);
        }
        refreshTagNames(questionId);
        Set<Long> affected = new LinkedHashSet<>(oldTagIds);
        if (tags != null) {
            tags.forEach(tag -> affected.add(tag.getId()));
        }
        refreshUsageCount(affected);
    }

    @Override
    @Transactional
    public void saveQuestionTags(long questionId, List<String> names, long userId) {
        replaceQuestionTags(questionId, resolveTags(names, userId));
    }

    @Override
    public void refreshTagNames(long questionId) {
        List<String> names = questionTagMapper.selectApprovedTagNames(questionId);
        String joined = names == null ? "" : String.join(",", names);
        questionMapper.updateQuestionTag(questionId, joined);
    }

    @Override
    public List<Tag> tagsOfQuestion(long questionId) {
        List<QuestionTag> relations = questionTagMapper.selectByQuestionId(questionId);
        if (relations.isEmpty()) {
            return List.of();
        }
        Set<Long> tagIds = relations.stream().map(QuestionTag::getTagId).collect(Collectors.toSet());
        return tagMapper.selectListByIds(tagIds);
    }

    @Override
    public List<QuestionTagDto> tagsForDetail(long questionId, Long viewerId, boolean author) {
        // 作者本人可见本帖的待审核标签，其他访问者只看到已通过标签
        List<Tag> tags = questionTagMapper.selectVisibleTags(questionId, author);
        return tags == null ? List.of() : tags.stream().map(this::toDto).toList();
    }

    @Override
    public Map<Long, List<QuestionTagDto>> approvedTagsOfQuestions(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        List<QuestionTag> relations = questionTagMapper.selectByQuestionIds(questionIds);
        if (relations.isEmpty()) {
            return Map.of();
        }
        Set<Long> tagIds = relations.stream().map(QuestionTag::getTagId).collect(Collectors.toSet());
        Map<Long, Tag> tagMap = new HashMap<>();
        for (Tag tag : tagMapper.selectListByIds(tagIds)) {
            if (tag.getStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
                tagMap.put(tag.getId(), tag);
            }
        }
        Map<Long, List<QuestionTagDto>> result = new LinkedHashMap<>();
        for (QuestionTag relation : relations) {
            Tag tag = tagMap.get(relation.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(relation.getQuestionId(), k -> new ArrayList<>()).add(toDto(tag));
        }
        return result;
    }

    @Override
    public void refreshUsageCount(Collection<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            if (tagId == null) {
                continue;
            }
            tagMapper.updateUsageCount(tagId, tagMapper.countUsage(tagId));
        }
    }

    @Override
    public List<Tag> approvedTags(int limit) {
        return tagMapper.selectApproved(0, Math.max(1, limit));
    }

    private QuestionTagDto toDto(Tag tag) {
        QuestionTagDto dto = new QuestionTagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setStatus(tag.getStatus());
        return dto;
    }
}
