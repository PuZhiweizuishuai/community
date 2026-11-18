package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:26
 */
@Data
@Table(value = "topics", camelToUnderline = false)
public class TagClass {
    @Id(keyType = KeyType.Auto)
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
