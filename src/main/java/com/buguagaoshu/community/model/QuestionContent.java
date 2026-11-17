package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 问题内容段落：一个问题最多 3 段，按 sort 升序展示。
 * type 见 QuestionContentTypeEnum；password 明文存储，仅密码可见段落使用。
 */
@Data
@Table(value = "question_content", camelToUnderline = false)
public class QuestionContent {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private long questionId;
    private int sort;
    private int type;
    private String content;
    private String password;
}