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
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:41
 */
@Component
public class TagClassCache {

    private record Snapshot(List<TagClass> list, Map<String, TagClass> map) {
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
        Map<String, TagClass> tagClassMap = new HashMap<>();
        for (TagClass tagClass : tagClassList) {
            tagClassMap.put(tagClass.getTitle(), tagClass);
        }
        snapshot = new Snapshot(List.copyOf(tagClassList), Map.copyOf(tagClassMap));
    }

    /** 尚未首次加载时返回 null，调用方需回退数据库 */
    public Map<String, TagClass> getTagClassMap() {
        Snapshot current = snapshot;
        return current == null ? null : current.map();
    }

    public List<TagClass> getTagClassList() {
        Snapshot current = snapshot;
        return current == null ? null : current.list();
    }
}