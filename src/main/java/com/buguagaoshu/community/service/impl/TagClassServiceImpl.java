package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.enums.TopicVisibilityEnum;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.service.TopicAccessService;
import com.buguagaoshu.community.util.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;


/**
 * 话题计数以数据库为准：先做原子增量，事务提交后再重建缓存；
 * 禁止在事务内原地修改共享缓存对象（回滚时缓存无法跟着回滚）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:35
 */
@Service
public class TagClassServiceImpl implements TagClassService {
    private final TagClassMapper tagClassMapper;

    private final TagClassCache tagClassCache;

    private final TopicAccessService topicAccessService;

    @Autowired
    public TagClassServiceImpl(TagClassMapper tagClassMapper, TagClassCache tagClassCache,
                              TopicAccessService topicAccessService) {
        this.tagClassMapper = tagClassMapper;
        this.tagClassCache = tagClassCache;
        this.topicAccessService = topicAccessService;
    }

    @Override
    public List<TagClass> getTagClassByTypeNotUserPage(String type) {
        int typeCode = 1;
        try {
            typeCode = Integer.parseInt(type);
            // 话题分类已改为后台动态维护，不再受固定枚举数量限制
            if (typeCode <= 0) {
                typeCode = 1;
            }
        } catch (Exception e) {
            typeCode = 1;
        }
        List<TagClass> tagClassList = new ArrayList<>();
        // 定时任务尚未首次执行时缓存可能为空，直接从数据库兜底
        List<TagClass> cached = tagClassCache.getTagClassList();
        if (cached == null) {
            cached = tagClassMapper.selectAll();
        }
        if (cached == null) {
            return tagClassList;
        }
        for (TagClass tagClass : cached) {
            // 公共话题列表：只展示已审核、启用且非私有隐藏的话题；私有可申请保留可见性标识
            if (tagClass.getType() == typeCode
                    && tagClass.getReviewStatus() == 1
                    && tagClass.getStatus() == 1
                    && tagClass.getVisibility() != TopicVisibilityEnum.PRIVATE_HIDDEN.getCode()) {
                tagClassList.add(tagClass);
            }
        }
        return tagClassList;
    }

    @Override
    public List<TagClass> searchEnabledTopics(String keyword, int limit, User viewer) {
        int max = Math.min(Math.max(limit, 1), 50);
        String kw = StringUtils.trimToNull(keyword);
        List<TagClass> cached = tagClassCache.getTagClassList();
        if (cached == null) {
            cached = tagClassMapper.selectAll();
        }
        if (cached == null) {
            return List.of();
        }
        Comparator<TagClass> byPopularity = Comparator
                .comparingLong(TagClass::getFollowCount).reversed()
                .thenComparing(Comparator.comparingLong(TagClass::getTalkCount).reversed())
                .thenComparingLong(TagClass::getId);
        String lower = kw == null ? null : kw.toLowerCase(Locale.ROOT);
        return cached.stream()
                .filter(topic -> topic.getStatus() == 1 && topic.getReviewStatus() == 1)
                // 私有话题只对成员 / 吧务 / 创建者 / 管理员展示
                .filter(topic -> topic.getVisibility() == TopicVisibilityEnum.PUBLIC.getCode()
                        || topicAccessService.canPostInTopic(topic.getId(), viewer))
                .filter(topic -> lower == null || (topic.getTitle() != null
                        && topic.getTitle().toLowerCase(Locale.ROOT).contains(lower)))
                .sorted(byPopularity)
                .limit(max)
                .toList();
    }

    @Override
    public PaginationDto<TagClass> searchTopics(String keyword, String page, String size, User viewer) {
        PaginationDto<TagClass> result = new PaginationDto<>();
        String kw = StringUtils.trimToNull(keyword);
        if (kw == null) {
            return result;
        }
        List<TagClass> cached = tagClassCache.getTagClassList();
        if (cached == null) {
            cached = tagClassMapper.selectAll();
        }
        if (cached == null) {
            return result;
        }
        Comparator<TagClass> byPopularity = Comparator
                .comparingLong(TagClass::getFollowCount).reversed()
                .thenComparing(Comparator.comparingLong(TagClass::getTalkCount).reversed())
                .thenComparingLong(TagClass::getId);
        String lower = kw.toLowerCase(Locale.ROOT);
        List<TagClass> matched = cached.stream()
                .filter(topic -> topic.getStatus() == 1 && topic.getReviewStatus() == 1)
                // 公开话题直接可见；私有话题仅在信息对搜索者可见时出现
                .filter(topic -> topic.getVisibility() == TopicVisibilityEnum.PUBLIC.getCode()
                        || topicAccessService.resolve(topic.getId(), viewer).visible())
                .filter(topic -> topic.getTitle() != null
                        && topic.getTitle().toLowerCase(Locale.ROOT).contains(lower))
                .sorted(byPopularity)
                .toList();
        long[] param = NumberUtils.getPageAndSize(page, size, matched.size());
        int from = (int) Math.min(param[0], (long) matched.size());
        int to = (int) Math.min(from + param[1], (long) matched.size());
        result.setData(matched.subList(from, to));
        result.setAllCount(matched.size());
        result.setPagination(param[2], param[3], param[1]);
        return result;
    }

    @Override
    public int updateTalkCount(long topicId, int count) {
        if (topicId <= 0 || count == 0) {
            return 0;
        }
        TagClass delta = new TagClass();
        delta.setId(topicId);
        delta.setTalkCount(count);
        int rows = tagClassMapper.updateTagTalkCount(delta);
        if (rows > 0) {
            refreshCacheAfterCommit();
        }
        return rows;
    }

    @Override
    public int updateFollowCount(TagClass tagClass) {
        int result = tagClassMapper.updateTagFollowCount(tagClass);
        if (result > 0) {
            refreshCacheAfterCommit();
        }
        return result;
    }

    @Override
    public TagClass findById(long topicId) {
        if (topicId <= 0) {
            return null;
        }
        TagClass cached = tagClassCache.getById(topicId);
        if (cached != null) {
            return cached;
        }
        return tagClassMapper.selectOneById(topicId);
    }

    @Override
    public TagClass findApprovedById(long topicId) {
        TagClass topic = findById(topicId);
        if (topic == null || topic.getReviewStatus() != 1 || topic.getStatus() != 1) {
            return null;
        }
        return topic;
    }

    /** 事务提交后再重建缓存；无事务上下文时立即重建 */
    private void refreshCacheAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tagClassCache.refresh();
                }
            });
        } else {
            tagClassCache.refresh();
        }
    }
}
