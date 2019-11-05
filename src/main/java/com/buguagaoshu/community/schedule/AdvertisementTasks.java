package com.buguagaoshu.community.schedule;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.model.Advertisement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


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
        List<Advertisement> advertisementList = advertisementMapper.selectAdvertisementByStatus(1);
        for (Advertisement advertisement : advertisementList) {
            switch (advertisement.getPosition()) {
                case HOME:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getHomeAdvertisementMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case PUBLISH:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getPublishAdvertisementMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case QUESTION:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getQuestionAdvertisementMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case USER:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getUserAdvertisementMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case USER_HOME:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getUserHomeAdvertisementMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case CLASS:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getClassAdvertisementMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case MESSAGE:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getMessageAdvertisementMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case NEWS:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getNewsMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                case SEARCH:
                    if (advertisement.getEndTime() >= System.currentTimeMillis()) {
                        advertisementCache.getSearchMap().put(advertisement.getId(), advertisement);
                    } else {
                        close(advertisement);
                    }
                    break;
                default:
            }
        }

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
