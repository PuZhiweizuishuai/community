package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:26
 */
@Data
public class TagClass {
    private long id;
    private String title;
    private long talkCount;
    private long followCount;
    private String image;
    private String simpleDesc;
    private int type;
    private long createTime;
    private long modifiedTime;
    private int status;
}
