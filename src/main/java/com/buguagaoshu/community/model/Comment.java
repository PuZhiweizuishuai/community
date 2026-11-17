package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 18:57
 */
@Data
@Table(value = "comment", camelToUnderline = false)
public class Comment {
    /**
     * 评论 id
     * */
    @Id(keyType = KeyType.Auto)
    private long commentId;

    /**
     * 评论所存在的问题
     * */
    private long questionId;

    /**
     * 评论在评论的位置
     * */
    private long parentCommentId;

    /**
     * 评论目标 id
     * */
    private long parentId;

    /**
     * 评论类型
     * */
    private int type;

    /**
     * 评论人
     * */
    private long commentator;

    /**
     * 内容
     * */
    private String content;

    /**
     * 点赞数
     * */
    private long likeCount;


    /**
     * 评论回复数
     * */
    private long commentCount;

    /**
     * 创建时间
     * */
    private long createTime;

    /**
     * 修改时间
     * */
    private long modifiedTime;

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
