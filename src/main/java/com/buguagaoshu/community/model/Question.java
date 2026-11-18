package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:44
 */
@Data
@Table(value = "questions", camelToUnderline = false)
public class Question {
    @Id(keyType = KeyType.Auto)
    private long questionId = -1;
    private long userId;
    private String title;
    /** 问题类型 code（classifications.kind=question） */
    private Integer classification;
    /** 所属话题（单选，topics.id） */
    private Long topicId;
    private String description;
    private String fileUrl;
    private long viewCount;
    private long commentCount;
    private long likeCount;
    private long followCount;
    /** 冗余展示列：由 QuestionTagService 维护为所有「已通过」标签名，全文索引依赖该列 */
    private String tag;
    /** 话题内置顶 */
    private int isTop;
    /** 加精 */
    private int isEssence;
    private long createTime;
    private long alterTime;

    /**
     * 状态，删除 或 没删除
     * */
    private int status;

    /**
     * 审核状态：0 待审核，1 已通过，2 未通过（存量数据默认已通过）
     * */
    private int reviewStatus = 1;

    /**
     * 审核不通过原因
     * */
    private String reviewReason;

    /**
     * 管理员修改意见
     * */
    private String reviewSuggestion;

    /**
     * 最近一次审核的管理员
     * */
    private long reviewerId;

    /**
     * 最近一次审核时间
     * */
    private long reviewTime;
}
