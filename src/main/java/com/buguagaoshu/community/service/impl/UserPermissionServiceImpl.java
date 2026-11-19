package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.mapper.UserPermissionMapper;
import com.buguagaoshu.community.service.UserPermissionService;
import com.mybatisflex.core.util.UpdateEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 19:23
 */
@Service
public class UserPermissionServiceImpl implements UserPermissionService {
    private final UserPermissionMapper userPermissionMapper;

    @Autowired
    public UserPermissionServiceImpl(UserPermissionMapper userPermissionMapper) {
        this.userPermissionMapper = userPermissionMapper;
    }


    @Override
    public UserPermission selectUserPermissionById(long id) {
        return userPermissionMapper.selectOneById(id);
    }

    @Override
    public int insertUserPermission(UserPermission userPermission) {
        return userPermissionMapper.insertSelectiveWithPk(userPermission);
    }

    @Override
    public int deleteUserPermissionById(long id) {
        return userPermissionMapper.deleteById(id);
    }

    @Override
    public int updateUserPermissionById(long id, int power, String modifier, long updateTime) {
        UserPermission update = UpdateEntity.of(UserPermission.class, id);
        update.setPower(power);
        update.setModifier(modifier);
        update.setUpdateTime(updateTime);
        return userPermissionMapper.update(update);
    }

    @Override
    public int updateDueTime(long id, int power, String modifier, long updateTime, long dueTime) {
        UserPermission update = UpdateEntity.of(UserPermission.class, id);
        update.setPower(power);
        update.setModifier(modifier);
        update.setUpdateTime(updateTime);
        update.setDueTime(dueTime);
        return userPermissionMapper.update(update);
    }
}