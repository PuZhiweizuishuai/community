package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 20:05
 */
@Data
public class Advertisement {
    private long id;
    private String title;
    private String url;
    private String image;
    private long createTime;
    private long modifiedUser;
    private long startTime;
    private long endTime;

    /**
     * 广告投放位置
     * 取值建议
     * home page
     * publish page
     * */
    private String position;
    private long viewCount;
    private int status;
}
