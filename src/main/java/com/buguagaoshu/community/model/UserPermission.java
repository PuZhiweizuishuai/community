package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:04
 * 用户权限
 */
@Data
@Table(value = "userPermission", camelToUnderline = false)
public class UserPermission {
    @Id(keyType = KeyType.None)
    private long id;
    /**
     * 用户等级
     * 1 普通用户
     * 2 vip
     * 管理员身份已迁移到 RBAC 角色（sys_user_role），不再使用 power 判断
     * */
    private int power;

    /**
     * 修改人
     * 0 为初始创建
     * */
    private String modifier;

    /**
     * 修改时间
     * */
    private String updateTime;

    /**
     * Vip 到期时间
     * */
    private long dueTime;

    public UserPermission() {

    }

    public UserPermission(long id, int power, String modifier, String updateTime, long dueTime) {
        this.id = id;
        this.power = power;
        this.modifier = modifier;
        this.updateTime = updateTime;
        this.dueTime = dueTime;
    }
}
