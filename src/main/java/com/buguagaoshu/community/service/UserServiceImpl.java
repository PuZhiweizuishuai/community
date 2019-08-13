package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.User;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 13:38
 */
@Service
public class UserServiceImpl implements UserService {
    /**
     * 注入 MyBatis 映射对象
     */
    private final UserMapper userMapper;

    @Autowired
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User selectUserById(long id) {
        return userMapper.selectUserById(id);
    }

    @Override
    public User selectUserByEmail(String email) {
        return userMapper.selectUserByEmail(email);
    }

    @Override
    public int deleteUserById(long id) {
        return userMapper.deleteUserById(id);
    }

    /**
     * @return -1 邮箱重复
     *  0 错误
     *  1 成功
     * */
    @Override
    public int insertUser(User user) {
        if(selectUserByEmail(user.getEmail()) != null) {
            return -1;
        }
        // 加密密码
        user.setPassword(StringUtil.BCryptPassword(user.getPassword()));
        return userMapper.insertUser(user);
    }

    @Override
    public int updateUserNameById(long id, String newUserName) {
        return userMapper.updateUserNameById(id, newUserName);
    }

    /**
     * @return -1 邮箱重复
     * */
    @Override
    public int updateUserEmailById(long id, String newEmail) {
        if(selectUserByEmail(newEmail) != null) {
            return -1;
        }
        return userMapper.updateUserEmailById(id, newEmail);
    }

    @Override
    public int updateUserPasswordById(long id, String newPassword) {
        return userMapper.updateUserPasswordById(id, StringUtil.BCryptPassword(newPassword));
    }

    @Override
    public int updateUserLastTimeById(long id, String lastTime) {
        return userMapper.updateUserLastTimeById(id, lastTime);
    }

    @Override
    public int updateUserHeadUrlById(long id, String headUrl) {
        return userMapper.updateUserHeadUrlById(id, headUrl);
    }
}
