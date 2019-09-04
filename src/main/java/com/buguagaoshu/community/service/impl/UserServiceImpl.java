package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public User selectUserByUserId(String userId) {
        User user = userMapper.selectUserByUserId(userId);
        if (user == null) {
            return null;
        }
        user.clean();
        return user;
    }

    @Override
    public int deleteUserById(long id) {
        return userMapper.deleteUserById(id);
    }

    /**
     * @return -2 账号重复
     * 0 错误
     * 1 成功
     */
    @Override
    public int insertUser(User user) {
        if (selectUserByEmail(user.getEmail()) != null) {
            return -1;
        }
        if (selectUserByUserId(user.getUserId()) != null) {
            return -2;
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
     */
    @Override
    public int updateUserEmailById(long id, String newEmail) {
        if (selectUserByEmail(newEmail) != null) {
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

    @Override
    public PaginationDto<User> searchUser(String search, String page, String size) {
        long allUserCount = userMapper.searchUserCount(search);
        long[] param = NumberUtils.getPageAndSize(page, size, allUserCount);
        List<User> userList = userMapper.searchUser(search, param[0], param[1]);
        for(User user : userList) {
            user.clean();
        }
        PaginationDto<User> paginationDto = new PaginationDto<>();
        paginationDto.setData(userList);
        paginationDto.setPagination(param[2], param[3], param[1]);

        paginationDto.setAllCount(allUserCount);
        return paginationDto;
    }
}
