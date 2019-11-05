package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.dto.CreateAdvertisement;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.model.Advertisement;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdvertisementService;
import com.buguagaoshu.community.util.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 20:48
 */
@Service
public class AdvertisementServiceImpl implements AdvertisementService {
    private final static long DAY_TIME = 86400000L;

    private final AdvertisementMapper advertisementMapper;

    private final AdvertisementCache advertisementCache;

    @Autowired
    public AdvertisementServiceImpl(AdvertisementMapper advertisementMapper, AdvertisementCache advertisementCache) {
        this.advertisementMapper = advertisementMapper;
        this.advertisementCache = advertisementCache;
    }


    @Override
    public int insertAdvertisement(CreateAdvertisement createAdvertisement, User user) {
        if (StringUtils.isNotBlank(createAdvertisement.getTitle())
                && StringUtils.isNotBlank(createAdvertisement.getUrl())
                && StringUtils.isNotBlank(createAdvertisement.getImage())) {
            Advertisement advertisement = new Advertisement();
            advertisement.setTitle(createAdvertisement.getTitle());
            advertisement.setUrl(createAdvertisement.getUrl());
            advertisement.setImage(createAdvertisement.getImage());
            advertisement.setCreateTime(System.currentTimeMillis());
            advertisement.setModifiedUser(user.getId());
            advertisement.setPosition("");
            advertisement.setStartTime(0);
            advertisement.setEndTime(0);
            advertisement.setStatus(0);
            return advertisementMapper.insertAdvertisement(advertisement);
        }
        return 0;
       // return advertisementMapper.insertAdvertisement(advertisement);
    }

    @Override
    public int alterAdvertisement(CreateAdvertisement createAdvertisement, User user) {
        Advertisement advertisement = advertisementMapper.selectAdvertisementById(createAdvertisement.getId());
        if (StringUtils.isNotBlank(createAdvertisement.getTitle())
                && StringUtils.isNotBlank(createAdvertisement.getUrl())
                && StringUtils.isNotBlank(createAdvertisement.getImage())) {
            advertisement.setTitle(createAdvertisement.getTitle());
            advertisement.setUrl(createAdvertisement.getUrl());
            advertisement.setImage(createAdvertisement.getImage());
            advertisement.setModifiedUser(user.getId());
            return advertisementMapper.alterAdvertisement(advertisement);
        }
        return 0;
    }

    @Override
    public Advertisement selectAdvertisementById(long id) {
        return advertisementMapper.selectAdvertisementById(id);
    }

    @Override
    public PaginationDto<Advertisement> getAdvertisementList(String page, String size) {
        long allAdvertisementCount = advertisementMapper.selectAdvertisementCount();
        long[] param = NumberUtils.getPageAndSize(page, size, allAdvertisementCount);
        List<Advertisement> advertisementList = advertisementMapper.selectAdvertisementList(param[0], param[1]);
        PaginationDto<Advertisement> paginationDto = new PaginationDto<>();
        paginationDto.setData(advertisementList);
        if (param[2] == 0) {
            param[2] = 1;
        }
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allAdvertisementCount);
        return paginationDto;
    }

    @Override
    public String updateAdvertisementViewCount(String id) {
        long adId = -1;
        try {
            adId = Long.parseLong(id);
        } catch (Exception e) {
            adId = -1;
        }
        if (adId == -1) {
            return "";
        }
        Advertisement advertisement = advertisementMapper.selectAdvertisementById(adId);
        if (advertisement != null) {
            advertisement.setViewCount(1);
            advertisementMapper.updateAdvertisementViewCount(advertisement);
            return advertisement.getUrl();
        }
        return "";
    }

    @Override
    public int settingAdvertisement(CreateAdvertisement createAdvertisement, User user) {
        if (createAdvertisement.getId() != null && createAdvertisement.getTime() != null && createAdvertisement.getPosition() != null) {
            Advertisement advertisement = advertisementMapper.selectAdvertisementById(createAdvertisement.getId());
            if (advertisement != null) {
                if (createAdvertisement.getPosition().equals(AdvertisementCache.HOME)) {
                    if (advertisementCache.getHomeAdvertisementMap().size() >= advertisementCache.AD_MAX_NUMBER) {
                        return 2;
                    }
                    advertisement.setPosition(createAdvertisement.getPosition());
                    advertisement.setStatus(1);
                    advertisement.setStartTime(System.currentTimeMillis());
                    advertisement.setEndTime(System.currentTimeMillis() + createAdvertisement.getTime() * DAY_TIME);
                    advertisement.setModifiedUser(user.getId());

                    advertisementCache.getHomeAdvertisementMap().put(advertisement.getId(), advertisement);
                    return advertisementMapper.updateAdvertisementSetting(advertisement);
                } else if (createAdvertisement.getPosition().equals(AdvertisementCache.PUBLISH)) {
                    if (advertisementCache.getPublishAdvertisementMap().size() >= advertisementCache.AD_MAX_NUMBER) {
                        return 2;
                    }
                    advertisement.setPosition(createAdvertisement.getPosition());
                    advertisement.setStatus(1);
                    advertisement.setStartTime(System.currentTimeMillis());
                    advertisement.setEndTime(System.currentTimeMillis() + createAdvertisement.getTime() * DAY_TIME);
                    advertisement.setModifiedUser(user.getId());
                    advertisementCache.getPublishAdvertisementMap().put(advertisement.getId(), advertisement);
                    return advertisementMapper.updateAdvertisementSetting(advertisement);
                } else if (createAdvertisement.getPosition().equals(AdvertisementCache.QUESTION)) {
                    if (advertisementCache.getQuestionAdvertisementMap().size() >= advertisementCache.AD_MAX_NUMBER) {
                        return 2;
                    }
                    advertisement.setPosition(createAdvertisement.getPosition());
                    advertisement.setStatus(1);
                    advertisement.setStartTime(System.currentTimeMillis());
                    advertisement.setEndTime(System.currentTimeMillis() + createAdvertisement.getTime() * DAY_TIME);
                    advertisement.setModifiedUser(user.getId());
                    advertisementCache.getQuestionAdvertisementMap().put(advertisement.getId(), advertisement);
                    return advertisementMapper.updateAdvertisementSetting(advertisement);
                }

            } else {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public int closeAdvertisement(CreateAdvertisement createAdvertisement, User user) {
        Advertisement advertisement = advertisementMapper.selectAdvertisementById(createAdvertisement.getId());
        if (advertisement != null) {
            advertisementCache.removeAdvertisement(advertisement);
            advertisement.setStatus(0);
            advertisement.setModifiedUser(user.getId());
            advertisement.setEndTime(0);
            advertisement.setStartTime(0);
            advertisement.setPosition("");
            return advertisementMapper.updateAdvertisementSetting(advertisement);
        }
        return 0;
    }

}
