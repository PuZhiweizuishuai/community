package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.UserPermission;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:22
 */
public interface UserPermissionService {
    /**
     * 查找用户权限
     * @param id 用户id
     * @return 返回权限
     * */
    UserPermission selectUserPermissionById(long id);

    /**
     * 插入用户权限
     * @param userPermission 用户权限
     * @return 结果
     * */
    int insertUserPermission(UserPermission userPermission);

    /**
     * 删除用户权限
     * @param id 用户id
     * @return 结果
     * */
    int deleteUserPermissionById(long id);

    /**
     * 修改用户权限
     * @param id 用户id
     * @param power 权限值
     * @return 结果
     * */
    int updateUserPermissionById(long id, int power,  String modifier, String updateTime);
}
