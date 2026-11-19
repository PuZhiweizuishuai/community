package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.component.RedisSchedulerLock;
import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.model.Advertisement;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import static com.buguagaoshu.community.cache.AdvertisementCache.*;
import static com.buguagaoshu.community.model.table.AdvertisementTableDef.ADVERTISEMENT;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-04 21:19
 */
@Component
@Slf4j
public class AdvertisementTasks {
    private final AdvertisementMapper advertisementMapper;


    private final AdvertisementCache advertisementCache;

    private final RedisSchedulerLock schedulerLock;

    @Autowired
    public AdvertisementTasks(AdvertisementMapper advertisementMapper, AdvertisementCache advertisementCache,
                              RedisSchedulerLock schedulerLock) {
        this.advertisementMapper = advertisementMapper;
        this.advertisementCache = advertisementCache;
        this.schedulerLock = schedulerLock;
    }

    /**
     * 一小时重建一次：按位置构建新 Map 后整体替换，
     * 已过期/已结束的广告不会残留在缓存里。
     * */
    @Scheduled(fixedRate = 3600000)
    public void setAdvertisement() {
        if (!schedulerLock.tryLock("advertisement", Duration.ofMinutes(10))) {
            return;
        }
        try {
            rebuildCache();
        } finally {
            schedulerLock.unlock("advertisement");
        }
    }

    private void rebuildCache() {
        long now = System.currentTimeMillis();
        // 过期广告用一条范围 UPDATE 统一关闭，避免逐条回写
        advertisementMapper.closeExpired(now);
        List<Advertisement> advertisementList = advertisementMapper.selectListByQuery(QueryWrapper.create()
                .where(ADVERTISEMENT.STATUS.eq(1)).and(ADVERTISEMENT.END_TIME.ge(now))
                .orderBy(ADVERTISEMENT.ID.desc()));
        Map<String, Map<Long, Object>> byPosition = new HashMap<>();
        for (Advertisement advertisement : advertisementList) {
            byPosition.computeIfAbsent(advertisement.getPosition(), p -> new ConcurrentHashMap<>())
                    .put(advertisement.getId(), advertisement);
        }
        advertisementCache.setHomeAdvertisementMap(byPosition.getOrDefault(HOME, new ConcurrentHashMap<>()));
        advertisementCache.setPublishAdvertisementMap(byPosition.getOrDefault(PUBLISH, new ConcurrentHashMap<>()));
        advertisementCache.setQuestionAdvertisementMap(byPosition.getOrDefault(QUESTION, new ConcurrentHashMap<>()));
        advertisementCache.setClassAdvertisementMap(byPosition.getOrDefault(CLASS, new ConcurrentHashMap<>()));
        advertisementCache.setUserAdvertisementMap(byPosition.getOrDefault(USER, new ConcurrentHashMap<>()));
        advertisementCache.setUserHomeAdvertisementMap(byPosition.getOrDefault(USER_HOME, new ConcurrentHashMap<>()));
        advertisementCache.setMessageAdvertisementMap(byPosition.getOrDefault(MESSAGE, new ConcurrentHashMap<>()));
        advertisementCache.setNewsMap(byPosition.getOrDefault(NEWS, new ConcurrentHashMap<>()));
        advertisementCache.setSearchMap(byPosition.getOrDefault(SEARCH, new ConcurrentHashMap<>()));
        log.info("加载广告数据完成！");
    }
}
