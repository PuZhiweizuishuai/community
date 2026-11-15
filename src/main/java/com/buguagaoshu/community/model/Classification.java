package com.buguagaoshu.community.model;

import lombok.Data;

/**
 * 动态分类：问题分类（提问/讨论/…）与话题所属分类（开发语言/平台框架/…）
 * 统一存放在 classification 表，由后台管理员维护。
 *
 * <p>code 是稳定数值标识：问题分类用于首页 /?class={code}，话题分类写入 topic.type。
 */
@Data
public class Classification {
    /** question = 问题分类；topic = 话题所属分类 */
    public static final String KIND_QUESTION = "question";
    public static final String KIND_TOPIC = "topic";

    private Long id;
    private String kind;
    private Integer code;
    private String name;
    private Integer sortOrder = 0;
    private Integer status = 1;
    private Long createTime;
    private Long modifiedTime;

    /** 仅用于接口返回：该分类下关联的问题数 / 话题数 */
    private Long usageCount;
}
