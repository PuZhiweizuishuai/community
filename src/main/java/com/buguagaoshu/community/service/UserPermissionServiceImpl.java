package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.UserPermission;
import com.buguagaoshu.community.mapper.UserPermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:23
 */
@Service
public class UserPermissionServiceImpl implements UserPermissionService{
    private final UserPermissionMapper userPermissionMapper;

    @Autowired
    public UserPermissionServiceImpl(UserPermissionMapper userPermissionMapper) {
        this.userPermissionMapper = userPermissionMapper;
    }


    @Override
    public UserPermission selectUserPermissionById(long id) {
        return userPermissionMapper.selectUserPermissionById(id);
    }

    @Override
    public int insertUserPermission(UserPermission userPermission) {
        return userPermissionMapper.insertUserPermission(userPermission);
    }

    @Override
    public int deleteUserPermissionById(long id) {
        return userPermissionMapper.deleteUserPermissionById(id);
    }

    @Override
    public int updateUserPermissionById(long id, int power, String modifier, String updateTime) {
        return userPermissionMapper.updateUserPermissionById(id, power, modifier, updateTime);
    }
}
