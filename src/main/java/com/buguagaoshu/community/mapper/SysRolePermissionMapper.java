package com.buguagaoshu.community.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Mapper
@Component(value = "SysRolePermissionMapper")
public interface SysRolePermissionMapper {

    @Select("SELECT permissionId FROM sys_role_permissions WHERE roleId = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") long roleId);

    @Delete("DELETE FROM sys_role_permissions WHERE roleId = #{roleId}")
    int deleteByRoleId(@Param("roleId") long roleId);

    @Insert("INSERT IGNORE INTO sys_role_permissions(roleId, permissionId) VALUES(#{roleId}, #{permissionId})")
    int insertIgnore(@Param("roleId") long roleId, @Param("permissionId") long permissionId);

    @Select("SELECT roleId, COUNT(*) AS total FROM sys_role_permissions GROUP BY roleId")
    List<Map<String, Object>> countPermissionsByRole();
}
