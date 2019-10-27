package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.FollowUserMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.FollowUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.FollowUserService;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 16:46
 */
@Service
public class FollowUserServiceImpl implements FollowUserService {
    private final FollowUserMapper followUserMapper;

    private final UserMapper userMapper;

    public FollowUserServiceImpl(FollowUserMapper followUserMapper, UserMapper userMapper) {
        this.followUserMapper = followUserMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = CustomizeException.class)
    public int insertFollowUser(long followUserId, Claims claims, User follow) {
        FollowUser followUser = new FollowUser();
        followUser.setFollowUserId(followUserId);
        followUser.setUserId(Long.parseLong(claims.getId()));
        followUser.setCreateTime(System.currentTimeMillis());
        FollowUser temp = followUserMapper.selectFollowUser(followUser);
        if (temp == null) {
            // 更新粉丝数
            follow.setFansCount(1);
            userMapper.updateUserFansCount(follow);
            // 更新关注数
            User user = new User();
            user.setId(Long.parseLong(claims.getId()));
            user.setFollowCount(1);
            userMapper.updateUserFollowCount(user);
            // 插入关注表
            followUserMapper.insertFollowUser(followUser);
            return 1;
        } else {
            // 更新粉丝数
            follow.setFansCount(-1);
            userMapper.updateUserFansCount(follow);
            // 更新关注数
            User user = new User();
            user.setId(Long.parseLong(claims.getId()));
            user.setFollowCount(-1);
            userMapper.updateUserFollowCount(user);
            // 删除
            followUserMapper.deleteFollowUser(temp);
            return 2;
        }
    }

    @Override
    public boolean isFollowUser(FollowUser followUser) {
        FollowUser temp = followUserMapper.selectFollowUser(followUser);
        return temp != null;
    }

    @Override
    public PaginationDto<User> getFollowUserList(long userId, String page, String size) {
        return null;
    }

    @Override
    public PaginationDto<User> getFansList(long userId, String page, String size) {
        return null;
    }
}
