package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.AdvertisementBatchDTO;
import com.buguagaoshu.community.dto.AdvertisementOperationResult;
import com.buguagaoshu.community.dto.CreateAdvertisement;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.Advertisement;
import com.buguagaoshu.community.model.User;

import java.util.List;
import java.util.Map;

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

    /** 批量投放：逐条返回成功/失败原因，位置容量按本次批次累计校验 */
    List<AdvertisementOperationResult> batchSetting(AdvertisementBatchDTO batch, User user);

    /** 批量结束投放 */
    List<AdvertisementOperationResult> batchClose(List<Long> ids, User user);

    /** 各投放位置的使用情况（总数 / 已用 / 剩余） */
    List<Map<String, Object>> positionStats();
}
