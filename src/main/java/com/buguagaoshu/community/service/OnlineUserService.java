package com.buguagaoshu.community.service;

import com.buguagaoshu.community.model.OnlineUser;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-14 19:14
 */
public interface OnlineUserService {
    /**
     * 查找当前用户是否在线
     * @param id 用户id
     * @return 结果
     * */
    OnlineUser selectOnlineUserById(long id);

    /**
     * 查找当前用户是否已经登陆
     * @param token 用户token
     * @return 结果
     * */
    OnlineUser selectOnlineUserByToken(String token);

    /**
     * @return 返回在线人数
     * */
    long selectOnlineUserCount();

    /**
     * 插入在线记录
     * @param user 在线用户
     * @return 结果
     * */
    int insertOnlineUser(OnlineUser user);

    /**
     * 删除用户在线记录
     * @param id 用户id
     * @return 结果
     * */
    int deleteOnlineUserById(long id);

    /**
     * 删除用户在线记录
     * @param token 用户token
     * @return 结果
     * */
    int deleteOnlineUserByToken(String token);
}
