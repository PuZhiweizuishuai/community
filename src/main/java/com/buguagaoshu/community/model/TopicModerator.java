package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 话题吧务成员：owner=吧主，moderator=小吧。
 * 吧务是资源范围权限，不占用全局 RBAC 权限码。
 */
@Data
@Table(value = "topic_moderators", camelToUnderline = false)
public class TopicModerator {
    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_MODERATOR = "moderator";

    @Id(keyType = KeyType.Auto)
    private long id;
    private long topicId;
    private long userId;
    private String role = ROLE_MODERATOR;
    private long assignedBy;
    private long createTime;
}
