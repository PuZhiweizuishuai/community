package com.buguagaoshu.community.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户主页隐私设置。
 */
@Data
public class UserPrivacySettingsDTO {

    @NotNull(message = "请设置是否公开关注的问题")
    private Boolean showFollowQuestions;

    @NotNull(message = "请设置是否公开关注的话题")
    private Boolean showFollowTopics;

    @NotNull(message = "请设置是否公开粉丝列表")
    private Boolean showFans;

    @NotNull(message = "请设置是否公开关注的人")
    private Boolean showFollowingUsers;
}
