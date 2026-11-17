package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.TagClassService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.buguagaoshu.community.model.table.TagClassTableDef.TAG_CLASS;


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
    public int updateTalkCount(String tag, int count) {
        if (tag == null || tag.isBlank()) {
            return 0;
        }
        Set<String> tags = new HashSet<>(Arrays.asList(tag.split(",|，")));
        boolean updated = false;
        for (String tagStr : tags) {
            TagClass tagClass = findTag(tagStr);
            if (tagClass != null) {
                TagClass delta = new TagClass();
                delta.setId(tagClass.getId());
                delta.setTalkCount(count);
                tagClassMapper.updateTagTalkCount(delta);
                updated = true;
            }
        }
        if (updated) {
            refreshCacheAfterCommit();
        }
        return 1;
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
    public int alterQuestionTalkCount(String[] newQuestionTag, String[] oldQuestionTag) {
        Set<String> newTags = new HashSet<>(Arrays.asList(newQuestionTag));
        Set<String> oldTags = new HashSet<>(Arrays.asList(oldQuestionTag));
        boolean updated = false;
        for (String tag : newTags) {
            if (!oldTags.contains(tag)) {
                updated |= adjustTalkCount(tag, 1);
            }
        }
        for (String tag : oldTags) {
            if (!newTags.contains(tag)) {
                updated |= adjustTalkCount(tag, -1);
            }
        }
        if (updated) {
            refreshCacheAfterCommit();
        }
        return updated ? 1 : 0;
    }

    private boolean adjustTalkCount(String tag, int delta) {
        TagClass tagClass = findTag(tag);
        if (tagClass == null) {
            return false;
        }
        TagClass increment = new TagClass();
        increment.setId(tagClass.getId());
        increment.setTalkCount(delta);
        tagClassMapper.updateTagTalkCount(increment);
        return true;
    }

    private TagClass findTag(String title) {
        Map<String, TagClass> map = tagClassCache.getTagClassMap();
        TagClass tagClass = map == null ? null : map.get(title);
        return tagClass != null ? tagClass : tagClassMapper.selectOneByQuery(
                QueryWrapper.create().where(TAG_CLASS.TITLE.eq(title)));
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