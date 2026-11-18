package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 14:29
 */
@Data
@Table(value = "follow_questions", camelToUnderline = false)
public class FollowQuestion {
    @Id(keyType = KeyType.Auto)
    private long id;
    private long followQuestionId;
    private long followQuestionAuthor;
    private String followQuestionTitle;
    private long userId;
    private long createTime;
    @Column(ignore = true)
    private String time;
}
