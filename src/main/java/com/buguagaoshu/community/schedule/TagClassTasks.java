package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.component.RedisSchedulerLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:41
 */
@Component
@Slf4j
public class TagClassTasks {

    private final TagClassCache tagClassCache;

    private final RedisSchedulerLock schedulerLock;

    @Autowired
    public TagClassTasks(TagClassCache tagClassCache, RedisSchedulerLock schedulerLock) {
        this.tagClassCache = tagClassCache;
        this.schedulerLock = schedulerLock;
    }

    /**
     * 十分钟更新一次（多实例只允许一个实例执行）
     * */
    @Scheduled(fixedRate = 600000)
    public void scheduledRefresh() {
        if (!schedulerLock.tryLock("tagClass", Duration.ofMinutes(5))) {
            return;
        }
        try {
            TagClassCache();
        } finally {
            schedulerLock.unlock("tagClass");
        }
    }

    /** 后台手动刷新入口，不走分布式锁 */
    public void TagClassCache() {
        log.info("开始缓存话题");
        tagClassCache.refresh();
    }
}