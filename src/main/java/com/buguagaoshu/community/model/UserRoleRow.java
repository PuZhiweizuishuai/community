package com.buguagaoshu.community.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色展示行（批量装配用户角色时使用）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleRow {
    private long userId;
    private long roleId;
    private String roleCode;
    private String roleName;
}
