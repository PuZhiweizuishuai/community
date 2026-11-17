package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 15:59
 * 点赞
 */
@Data
@Table(value = "clickLike", camelToUnderline = false)
public class ClickLike {
    @Id(keyType = KeyType.Auto)
    private long likeId;
    private long notifier;
    private String notifierName;
    private long receiver;
    private long questionId;
    private long commentId;
    private int type;
    private long createTime;
    private long notificationId;
}
