package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * RBAC 角色。
 * <p>{@code builtIn = 1} 为系统内置角色（超级管理员），不可删除、权限不可编辑。
 */
@Data
@Table(value = "sys_roles", camelToUnderline = false)
public class SysRole {

    @Id(keyType = KeyType.Auto)
    private long id;

    /**
     * 角色编码，创建后不可修改
     */
    private String code;

    private String name;

    private String description;

    private int builtIn;

    /**
     * 1 启用，0 停用
     */
    private int status;

    private int sortOrder;

    private long createTime;

    private long updateTime;
}
