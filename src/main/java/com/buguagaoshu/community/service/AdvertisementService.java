package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.CreateAdvertisement;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.Advertisement;
import com.buguagaoshu.community.model.User;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 20:46
 */
public interface AdvertisementService {
    /**
     * 添加广告数据
     *
     * @param createAdvertisement 广告
     * @param user                设置人
     * @return 插入结果
     */
    int insertAdvertisement(CreateAdvertisement createAdvertisement, User user);


    /**
     * 修改广告
     *
     * @param createAdvertisement 广告
     * @param user 修改人
     * @return 插入结果
     */
    int alterAdvertisement(CreateAdvertisement createAdvertisement, User user);

    /**
     * 通过广告id查找广告
     *
     * @param id 广告id
     * @return 广告
     */
    Advertisement selectAdvertisementById(long id);

    /**
     * 查找广告列表
     *
     * @param page 页码
     * @param size 数量
     * @return 结果
     */
    PaginationDto<Advertisement> getAdvertisementList(String page, String size);

    /**
     * 更新阅读数
     *
     * @param id 广告id
     * @return 结果
     */
    String updateAdvertisementViewCount(String id);


    /**
     * 设置广告投放信息
     *
     * @param createAdvertisement 广告
     * @param user                设置人
     * @return 结果
     */
    int settingAdvertisement(CreateAdvertisement createAdvertisement, User user);


    /**
     * 提前结束广告投放
     *
     * @param createAdvertisement 广告
     * @param user                设置人
     * @return 结果
     */
    int closeAdvertisement(CreateAdvertisement createAdvertisement, User user);
}
