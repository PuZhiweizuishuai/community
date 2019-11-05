package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.model.Advertisement;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-04 21:17
 */
@Component
@Data
public class AdvertisementCache {
    /**
     * 主页广告
     * */
    public final static String HOME = "home page";

    /**
     * 发布页广告
     * */
    public final static String PUBLISH = "publish page";

    /**
     * 问题页
     * */
    public final static String QUESTION = "question page";

    /**
     * 广告数
     * */
    @Value("${advertisement.number}")
    public int AD_MAX_NUMBER;

    private Map<Long, Object> homeAdvertisementMap;
    private Map<Long, Object> publishAdvertisementMap;
    public Map<Long, Object> questionAdvertisementMap;

    public void removeAdvertisement(Advertisement advertisement) {
        if (advertisement.getPosition().equals(HOME)) {
            homeAdvertisementMap.remove(advertisement.getId());
        } else if (advertisement.getPosition().equals(PUBLISH)) {
            publishAdvertisementMap.remove(advertisement.getId());
        } else if (advertisement.getPosition().equals(QUESTION)) {
            questionAdvertisementMap.remove(advertisement.getId());
        }
    }
}
