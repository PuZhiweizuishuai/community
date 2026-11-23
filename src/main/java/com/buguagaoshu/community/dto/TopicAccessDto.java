package com.buguagaoshu.community.dto;

import com.buguagaoshu.community.model.TagClass;

/**
 * 当前访问者对某个话题的访问能力。
 *
 * @param topic        话题本身
 * @param visible      话题信息是否可见（false 时统一按 404 处理，避免泄露存在性）
 * @param canViewInfo  是否可以查看话题信息
 * @param canViewContent 是否可以查看话题内帖子
 * @param isMember     是否已加入话题
 * @param isOwner      是否吧主
 * @param isModerator  是否小吧
 * @param isAdmin      是否管理员
 * @param joinStatus   none / pending / invited / joined / rejected / public
 * @param joinType     apply / invite，无记录时为 null
 */
public record TopicAccessDto(TagClass topic,
                             boolean visible,
                             boolean canViewInfo,
                             boolean canViewContent,
                             boolean isMember,
                             boolean isOwner,
                             boolean isModerator,
                             boolean isAdmin,
                             String joinStatus,
                             String joinType) {
}
