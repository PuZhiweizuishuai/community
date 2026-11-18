package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 话题缓存：构建新集合后原子引用替换，刷新过程中读到的要么是旧快照要么是新快照，
 * 不会出现 clear-add 式的空窗。快照内的对象与集合只读，禁止调用方原地修改。
 *
 * <p>话题已改为单选外键，缓存同时提供 id 与 title 两种索引：
 * id 是主寻址方式，title 仅保留给旧链接/旧接口兼容。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:41
 */
@Component
public class TagClassCache {

    private record Snapshot(List<TagClass> list, Map<Long, TagClass> idMap, Map<String, TagClass> titleMap) {
    }

    private volatile Snapshot snapshot;

    private final TagClassMapper tagClassMapper;

    public TagClassCache(TagClassMapper tagClassMapper) {
        this.tagClassMapper = tagClassMapper;
    }

    /**
     * 从数据库全量重建缓存（定时任务与事务提交后调用）
     */
    public void refresh() {
        List<TagClass> tagClassList = tagClassMapper.selectAll();
        Map<Long, TagClass> idMap = new HashMap<>();
        Map<String, TagClass> titleMap = new HashMap<>();
        for (TagClass tagClass : tagClassList) {
            idMap.put(tagClass.getId(), tagClass);
            if (tagClass.getTitle() != null) {
                titleMap.put(tagClass.getTitle(), tagClass);
            }
        }
        snapshot = new Snapshot(List.copyOf(tagClassList), Map.copyOf(idMap), Map.copyOf(titleMap));
    }

    private Snapshot current() {
        Snapshot value = snapshot;
        if (value == null) {
            synchronized (this) {
                if (snapshot == null) {
                    refresh();
                }
                value = snapshot;
            }
        }
        return value;
    }

    /** 尚未首次加载时返回 null，调用方需回退数据库 */
    public Map<Long, TagClass> getTagClassMap() {
        Snapshot current = current();
        return current == null ? null : current.idMap();
    }

    public TagClass getById(long id) {
        Snapshot current = current();
        return current == null ? null : current.idMap().get(id);
    }

    public TagClass getByTitle(String title) {
        Snapshot current = current();
        return current == null || title == null ? null : current.titleMap().get(title);
    }

    public List<TagClass> getTagClassList() {
        Snapshot current = current();
        return current == null ? null : current.list();
    }
}
