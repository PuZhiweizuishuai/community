package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.mapper.OnlineUserMapper;
import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 19:15
 */
@Service
public class OnlineUserServiceImpl implements OnlineUserService {
    private final OnlineUserMapper onlineUserMapper;

    @Autowired
    public OnlineUserServiceImpl(OnlineUserMapper onlineUserMapper) {
        this.onlineUserMapper = onlineUserMapper;
    }

    @Override
    public OnlineUser selectOnlineUserById(long id) {
        return onlineUserMapper.selectOnlineUserById(id);
    }

    @Override
    public OnlineUser selectOnlineUserByToken(String token) {
        return onlineUserMapper.selectOnlineUserByToken(token);
    }

    @Override
    public long selectOnlineUserCount() {
        return onlineUserMapper.selectOnlineUserCount();
    }

    @Override
    public int insertOnlineUser(OnlineUser user) {
        // TODO 加入踢下线功能
        OnlineUser onlineUser = onlineUserMapper.selectOnlineUserById(user.getId());
        if (onlineUser == null) {
            return onlineUserMapper.insertOnlineUser(user);
        } else {
            onlineUserMapper.deleteOnlineUserById(user.getId());
            return onlineUserMapper.insertOnlineUser(user);
        }
    }

    @Override
    public int deleteOnlineUserById(long id) {
        return onlineUserMapper.deleteOnlineUserById(id);
    }

    @Override
    public int deleteOnlineUserByToken(String token) {
        return onlineUserMapper.deleteOnlineUserByToken(token);
    }
}
