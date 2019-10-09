package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Scheduled(fixedRate = 10800000)
    public void TagClassCache() {
        log.info("开始缓存话题");
        List<TagClass> tagClassList = tagClassMapper.getAllTagClass();
        tagClassCache.setTagClassList(tagClassList);
    }
}
