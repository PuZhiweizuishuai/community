package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.mapper.OnlineUserMapper;
import com.buguagaoshu.community.model.OnlineUser;
import com.buguagaoshu.community.service.OnlineUserService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.buguagaoshu.community.model.table.OnlineUserTableDef.ONLINE_USER;

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
        return onlineUserMapper.selectOneById(id);
    }

    @Override
    public OnlineUser selectOnlineUserByToken(String token) {
        return onlineUserMapper.selectOneByQuery(QueryWrapper.create().where(ONLINE_USER.TOKEN.eq(token)));
    }

    @Override
    public long selectOnlineUserCount() {
        return onlineUserMapper.selectCountByQuery(QueryWrapper.create());
    }

    @Override
    public int insertOnlineUser(OnlineUser user) {
        return onlineUserMapper.upsertOnlineUser(user);
    }

    @Override
    public int deleteOnlineUserById(long id) {
        return onlineUserMapper.deleteById(id);
    }

    @Override
    public int deleteOnlineUserByToken(String token) {
        return onlineUserMapper.deleteByQuery(QueryWrapper.create().where(ONLINE_USER.TOKEN.eq(token)));
    }

    @Override
    public int deleteExpireTimeUser(long expireTime) {
        return onlineUserMapper.deleteByQuery(QueryWrapper.create().where(ONLINE_USER.EXPIRE_TIME.le(expireTime)));
    }
}