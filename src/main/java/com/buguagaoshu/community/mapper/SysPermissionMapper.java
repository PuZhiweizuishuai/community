package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.SysPermission;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component(value = "SysPermissionMapper")
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询用户通过启用角色获得的全部权限编码（去重）。
     * 管理员鉴权与前端菜单渲染共用这一数据源。
     *
     * <p>超级管理员按角色编码兜底拥有全部权限，后续新增权限点无需再补授权数据。
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM sys_permissions p
                     JOIN sys_role_permissions rp ON rp.permissionId = p.id
                     JOIN sys_roles r ON r.id = rp.roleId AND r.status = 1
                     JOIN sys_user_roles ur ON ur.roleId = r.id
            WHERE ur.userId = #{userId}
            UNION
            SELECT p.code
            FROM sys_permissions p
            WHERE EXISTS (SELECT 1
                          FROM sys_user_roles ur2
                                   JOIN sys_roles r2 ON r2.id = ur2.roleId AND r2.status = 1
                          WHERE ur2.userId = #{userId}
                            AND r2.code = 'super_admin')
            """)
    List<String> selectCodesByUserId(@Param("userId") long userId);

    @Select("SELECT * FROM sys_permissions WHERE code = #{code}")
    SysPermission selectByCode(@Param("code") String code);
}
