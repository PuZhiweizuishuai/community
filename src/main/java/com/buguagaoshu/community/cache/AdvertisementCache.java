package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.model.Advertisement;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
     */
    public final static String HOME = "home page";

    /**
     * 发布页广告
     */
    public final static String PUBLISH = "publish page";

    /**
     * 问题页
     */
    public final static String QUESTION = "question page";

    /**
     * 分类页
     */
    public final static String CLASS = "class page";

    /**
     * 用户页
     */
    public final static String USER = "user page";

    /**
     * 个人主页
     */
    public final static String USER_HOME = "user home";

    /**
     * 消息通知页
     */
    public final static String MESSAGE = "message";

    /**
     * 搜索
     * */
    public final static String SEARCH = "search";

    /**
     * 首页新闻
     */
    public final static String NEWS = "news";

    /**
     * 广告数
     */
    @Value("${advertisement.number}")
    public int AD_MAX_NUMBER;

    private Map<Long, Object> homeAdvertisementMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> publishAdvertisementMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> questionAdvertisementMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> classAdvertisementMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> userAdvertisementMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> userHomeAdvertisementMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> messageAdvertisementMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> newsMap = new HashMap<>(AD_MAX_NUMBER);
    private Map<Long, Object> searchMap = new HashMap<>(AD_MAX_NUMBER);

    public void removeAdvertisement(Advertisement advertisement) {
        switch (advertisement.getPosition()) {
            case HOME:
                homeAdvertisementMap.remove(advertisement.getId());
                break;
            case PUBLISH:
                publishAdvertisementMap.remove(advertisement.getId());
                break;
            case QUESTION:
                questionAdvertisementMap.remove(advertisement.getId());
                break;
            case CLASS:
                classAdvertisementMap.remove(advertisement.getId());
                break;
            case USER:
                userAdvertisementMap.remove(advertisement.getId());
                break;
            case USER_HOME:
                userHomeAdvertisementMap.remove(advertisement.getId());
                break;
            case MESSAGE:
                messageAdvertisementMap.remove(advertisement.getId());
                break;
            case NEWS:
                newsMap.remove(advertisement.getId());
                break;
            case SEARCH:
                searchMap.remove(advertisement.getId());
                break;
            default:
        }
    }
}
