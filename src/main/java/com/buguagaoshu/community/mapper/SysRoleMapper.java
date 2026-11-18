package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.SysRole;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

@Mapper
@Component(value = "SysRoleMapper")
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT * FROM sys_role WHERE code = #{code}")
    SysRole selectByCode(@Param("code") String code);

    @Select("SELECT * FROM sys_role WHERE name = #{name}")
    SysRole selectByName(@Param("name") String name);
}
