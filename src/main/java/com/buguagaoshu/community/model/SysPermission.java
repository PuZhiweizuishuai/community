package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * 权限点定义。权限点由系统预置，后台按角色动态勾选。
 */
@Data
@Table(value = "sys_permission", camelToUnderline = false)
public class SysPermission {

    @Id(keyType = KeyType.Auto)
    private long id;

    /**
     * 权限编码，例如 admin:user
     */
    private String code;

    private String name;

    /**
     * 权限分组，后台按组展示
     */
    private String groupName;

    private String description;

    private int sortOrder;
}
