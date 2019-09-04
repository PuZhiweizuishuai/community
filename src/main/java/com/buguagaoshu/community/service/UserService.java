package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 13:34
 */
public interface UserService {
    /**
     * 通过 id 查询学生信息
     *
     * @param id 学号
     * @return 学生类
     */
    User selectUserById(long id);

    /**
     * 通过 email 查询学生信息
     *
     * @param email 学号
     * @return 学生类
     */
    User selectUserByEmail(String email);


    /**
     * 通过 userId 查询学生信息
     *
     * @param userId 用户名
     * @return 学生类
     */
    User selectUserByUserId(String userId);

    /**
     * 通过用户 ID 删除用户信息
     *
     * @param id 用户 ID
     * @return 行号
     */
    int deleteUserById(long id);

    /**
     * 添加新用户
     *
     * @param user 新用户类
     * @return 行号
     */
    int insertUser(User user);

    /**
     * 通过 id 更改用户名
     *
     * @param id          用户 ID
     * @param newUserName 新用户名
     * @return 1 成功
     */
    int updateUserNameById(long id, String newUserName);

    /**
     * 通过 id 更改邮箱
     *
     * @param id       用户 ID
     * @param newEmail 新邮箱
     * @return 1 成功
     */

    int updateUserEmailById(long id, String newEmail);

    /**
     * 通过 id 密码
     *
     * @param id          用户 ID
     * @param newPassword 新密码
     * @return 1 成功
     */
    int updateUserPasswordById(long id, String newPassword);

    /**
     * 通过 id 更改登陆时间
     *
     * @param id       用户 ID
     * @param lastTime 上次登陆时间
     * @return 1 成功
     */
    int updateUserLastTimeById(long id, String lastTime);

    /**
     * 通过 id 更改用户头像
     *
     * @param id      用户 ID
     * @param headUrl 新头像地址
     * @return 1 成功
     */
    int updateUserHeadUrlById(long id, String headUrl);


    PaginationDto<User> searchUser(String search, String page, String size);

}
