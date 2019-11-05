package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.model.Advertisement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.buguagaoshu.community.cache.AdvertisementCache.*;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-04 21:19
 */
@Component
@Slf4j
public class AdvertisementTasks {
    private final AdvertisementMapper advertisementMapper;


    private final AdvertisementCache advertisementCache;

    @Autowired
    public AdvertisementTasks(AdvertisementMapper advertisementMapper, AdvertisementCache advertisementCache) {
        this.advertisementMapper = advertisementMapper;
        this.advertisementCache = advertisementCache;
    }

    /**
     * 一小时更新一次
     * */
    @Scheduled(fixedRate = 3600000)
    public void setAdvertisement() {
        Map<Long, Object> home = new HashMap<>(4);
        Map<Long, Object> publish = new HashMap<>(4);
        Map<Long, Object> question = new HashMap<>(4);
        List<Advertisement> advertisementList = advertisementMapper.selectAdvertisementByStatus(1);
        for (Advertisement advertisement : advertisementList) {
            if (advertisement.getPosition().equals(HOME)) {
                if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                    home.put(advertisement.getId(), advertisement);
                } else {
                    close(advertisement);
                }
            } else if (advertisement.getPosition().equals(PUBLISH)) {
                if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                    publish.put(advertisement.getId(), advertisement);
                } else {
                    close(advertisement);
                }
            } else if (advertisement.getPosition().equals(QUESTION)) {
                if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                    question.put(advertisement.getId(), advertisement);
                } else {
                    close(advertisement);
                }
            }
        }
        advertisementCache.setHomeAdvertisementMap(home);
        advertisementCache.setPublishAdvertisementMap(publish);
        advertisementCache.setQuestionAdvertisementMap(question);
        log.info("加载广告数据完成！");
    }


    private void close(Advertisement advertisement) {
        advertisement.setStartTime(0);
        advertisement.setEndTime(0);
        advertisement.setModifiedUser(0);
        advertisement.setStatus(0);
        advertisementMapper.updateAdvertisementSetting(advertisement);
    }
}
