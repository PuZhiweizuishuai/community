package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 23:17
 */
@Data
@Table(value = "notifications", camelToUnderline = false)
public class Notification {
    /**
     * 通知主键 id
     */
    @Id(keyType = KeyType.Auto)
    private long id;

    /**
     * 通知发起人
     */
    private long notifier;

    /**
     * 通知接收人
     */
    private long receiver;

    /**
     * 通知产生点
     */
    private long outerId;

    private long commentId = -1;

    /**
     * 缓存评论内容
     * */
    private String commentContent = "";

    /**
     * 通知类型
     */
    private int type;

    /**
     * 通知创建时间
     */
    private long createTime;

    /**
     * 通知状态
     */
    private int status;
}
