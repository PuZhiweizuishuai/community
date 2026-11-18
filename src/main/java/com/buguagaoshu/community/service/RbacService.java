package com.buguagaoshu.community.service;

import com.buguagaoshu.community.model.SysPermission;
import com.buguagaoshu.community.model.SysRole;
import com.buguagaoshu.community.model.User;

import java.util.List;
import java.util.Map;

/**
 * RBAC 角色与权限管理。
 */
public interface RbacService {

    /** 注册用户默认角色编码 */
    String DEFAULT_ROLE_CODE = "user";

    record RoleSaveCommand(Long id, String code, String name, String description,
                           Integer status, Integer sortOrder) {
    }

    /** 角色列表（按 sortOrder 排序） */
    List<SysRole> listRoles();

    /** 角色视图：角色信息 + 权限数量 + 用户数量 */
    List<Map<String, Object>> listRoleViews();

    /** 新建或更新角色 */
    SysRole saveRole(RoleSaveCommand command);

    /** 删除角色（内置角色不可删除，关联关系级联清理） */
    void deleteRole(long roleId);

    /** 全部权限点（按分组、排序号排序） */
    List<SysPermission> listPermissions();

    List<Long> listRolePermissionIds(long roleId);

    /** 全量覆盖角色的权限集合；内置角色不可修改 */
    void saveRolePermissions(long roleId, List<Long> permissionIds);

    List<Long> listUserRoleIds(long userId);

    /** 全量覆盖用户的角色集合；禁止移除最后一个超级管理员 */
    void saveUserRoles(long userId, List<Long> roleIds);

    /** 按角色编码追加角色（注册时分配默认角色、迁移兜底） */
    void assignRoleByCode(long userId, String roleCode);

    /** 列表页批量装配用户角色名称 */
    void fillUserRoles(List<User> users);
}
