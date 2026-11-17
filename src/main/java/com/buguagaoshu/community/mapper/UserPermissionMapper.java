package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.UserPermission;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:12
 */
@Mapper
@Component(value = "UserPermissionMapper")
public interface UserPermissionMapper extends BaseMapper<UserPermission> {
}