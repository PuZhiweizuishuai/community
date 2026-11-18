package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 20:05
 */
@Data
@Table(value = "advertisements", camelToUnderline = false)
public class Advertisement {
    @Id(keyType = KeyType.Auto)
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
