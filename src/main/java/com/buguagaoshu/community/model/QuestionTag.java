package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 帖子 - 标签关联（联合主键 questionId + tagId）。
 */
@Data
@Table(value = "question_tags", camelToUnderline = false)
public class QuestionTag {
    @Id(keyType = KeyType.None)
    private long questionId;
    private long tagId;
}
