package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.TagClassService;
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

    @Autowired
    public TagClassServiceImpl(TagClassMapper tagClassMapper, TagClassCache tagClassCache) {
        this.tagClassMapper = tagClassMapper;
        this.tagClassCache = tagClassCache;
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
            if (tagClass.getType() == typeCode) {
                tagClassList.add(tagClass);
            }
        }
        return tagClassList;
    }

    @Override
    public List<TagClass> searchEnabledTopics(String keyword, int limit) {
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
                .filter(topic -> topic.getStatus() == 1)
                .filter(topic -> lower == null || (topic.getTitle() != null
                        && topic.getTitle().toLowerCase(Locale.ROOT).contains(lower)))
                .sorted(byPopularity)
                .limit(max)
                .toList();
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
