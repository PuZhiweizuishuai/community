package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.UserRoleRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
@Component(value = "SysUserRoleMapper")
public interface SysUserRoleMapper {

    @Select("SELECT roleId FROM sys_user_roles WHERE userId = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") long userId);

    @Delete("DELETE FROM sys_user_roles WHERE userId = #{userId}")
    int deleteByUserId(@Param("userId") long userId);

    @Insert("INSERT IGNORE INTO sys_user_roles(userId, roleId) VALUES(#{userId}, #{roleId})")
    int insertIgnore(@Param("userId") long userId, @Param("roleId") long roleId);

    @Select("SELECT COUNT(*) FROM sys_user_roles WHERE roleId = #{roleId}")
    long countUsersByRoleId(@Param("roleId") long roleId);

    @Select("SELECT roleId, COUNT(*) AS total FROM sys_user_roles GROUP BY roleId")
    List<Map<String, Object>> countUsersByRole();

    /**
     * 列表页批量装配用户角色，避免逐用户查询。
     */
    @Select("""
            <script>
            SELECT ur.userId AS userId, r.id AS roleId, r.code AS roleCode, r.name AS roleName
            FROM sys_user_roles ur
                     JOIN sys_roles r ON r.id = ur.roleId
            WHERE ur.userId IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY r.sortOrder, r.id
            </script>
            """)
    List<UserRoleRow> selectRoleNamesByUserIds(@Param("userIds") Collection<Long> userIds);
}
