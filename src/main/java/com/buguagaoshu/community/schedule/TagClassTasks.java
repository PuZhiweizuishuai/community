package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:41
 */
@Component
@Slf4j
public class TagClassTasks {
    private final TagClassMapper tagClassMapper;

    private final TagClassCache tagClassCache;

    @Autowired
    public TagClassTasks(TagClassMapper tagClassMapper, TagClassCache tagClassCache) {
        this.tagClassMapper = tagClassMapper;
        this.tagClassCache = tagClassCache;
    }

    /**
     * 十分钟更新一次
     * */
    @Scheduled(fixedRate = 600000)
    public void TagClassCache() {
        log.info("开始缓存话题");
        List<TagClass> tagClassList = tagClassMapper.getAllTagClass();
        Map<String, TagClass> tagClassMap = new HashMap<>();
        for (TagClass tagClass : tagClassList) {
            tagClassMap.put(tagClass.getTitle(), tagClass);
        }
        tagClassCache.setTagClassList(tagClassList);
        tagClassCache.setTagClassMap(tagClassMap);
    }
}
