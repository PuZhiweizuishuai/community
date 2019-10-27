package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.FollowUser;
import com.buguagaoshu.community.model.User;
import io.jsonwebtoken.Claims;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 16:41
 */
public interface FollowUserService {
    /**
     * 插入关注数据
     * @param followUserId 关注id
     * @param claims 当前用户信息
     * @return 结果
     * */
    int insertFollowUser(long followUserId, Claims claims, User follow);

    /**
     * 判断是否关注该用户
     * @param followUser 关注问题
     * @return 关注 true， 没关注 false
     * */
    boolean isFollowUser(FollowUser followUser);

    /**
     * 获取关注用户列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页显示数目
     * @return 结果
     * */
    PaginationDto<User> getFollowUserList(long userId, String page, String size);

    /**
     * 获取粉丝列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页显示数目
     * @return 结果
     * */
    PaginationDto<User> getFansList(long userId, String page, String size);
}
