package com.buguagaoshu.community.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Column;
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

    /** 可见性：0 公开，1 私有可申请，2 私有隐藏 */
    private int visibility;

    /** 创建人 userId，存量话题为 null；内部字段不对外暴露 */
    @JsonIgnore
    private Long createdBy;

    /** 审核状态：0 待审核，1 已通过，2 未通过 */
    private int reviewStatus = 1;

    private String reviewReason;

    @JsonIgnore
    private Long reviewerId;

    @JsonIgnore
    private Long reviewTime;

    /** 后台列表展示创建人昵称，不落库 */
    @Column(ignore = true)
    private String creatorName;
}
