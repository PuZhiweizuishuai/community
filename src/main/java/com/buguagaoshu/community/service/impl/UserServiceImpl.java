package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.mapper.UserPermissionMapper;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.RbacService;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.buguagaoshu.community.model.table.UserTableDef.USER;
import java.util.stream.Collectors;

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


    private final UserPermissionMapper userPermissionMapper;

    private final RbacService rbacService;

    private final UserBanService userBanService;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserPermissionMapper userPermissionMapper,
                           RbacService rbacService, UserBanService userBanService) {
        this.userMapper = userMapper;
        this.userPermissionMapper = userPermissionMapper;
        this.rbacService = rbacService;
        this.userBanService = userBanService;
    }

    @Override
    public User selectUserById(long id) {
        return userMapper.selectOneById(id);
    }

    @Override
    public User selectUserByEmail(String email) {
        return userMapper.selectOneByQuery(QueryWrapper.create().where(USER.EMAIL.eq(email)));
    }

    @Override
    public User selectUserByUserId(String userId) {
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where(USER.USER_ID.eq(userId)));
        if (user == null) {
            return null;
        }
        user.clean();
        return user;
    }

    @Override
    public int deleteUserById(long id) {
        return userMapper.deleteById(id);
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
        try {
            return userMapper.insertUser(user);
        } catch (DuplicateKeyException e) {
            // 并发注册撞唯一索引（userId/email），按重复处理
            return selectUserByEmail(user.getEmail()) != null ? -1 : -2;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int register(User user) {
        int result = insertUser(user);
        if (result != 1) {
            return result;
        }
        // 用户、权限、默认角色必须同时成功，否则会出现无法登录的账号
        UserPermission userPermission = new UserPermission(user.getId(), 1, "0",
                user.getCreationTime(), user.getCreationTime());
        userPermissionMapper.insertSelectiveWithPk(userPermission);
        rbacService.assignRoleByCode(user.getId(), RbacService.DEFAULT_ROLE_CODE);
        return 1;
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
        try {
            return userMapper.updateUserEmailById(id, newEmail);
        } catch (DuplicateKeyException e) {
            return -1;
        }
    }

    @Override
    public int updateUserPasswordById(long id, String newPassword) {
        return userMapper.updateUserPasswordById(id, StringUtil.BCryptPassword(newPassword));
    }

    @Override
    public int updateUserLastTimeById(long id, long lastTime) {
        return userMapper.updateUserLastTimeById(id, lastTime);
    }

    @Override
    public int updateUserHeadUrlById(long id, String headUrl) {
        return userMapper.updateUserHeadUrlById(id, headUrl);
    }

    @Override
    public PaginationDto<User> searchUser(String search, String page, String size) {
        String keyword = StringUtil.sanitizeFulltextKeyword(search);
        if (keyword == null) {
            return emptyPage(page, size);
        }
        long allUserCount = userMapper.searchUserCount(keyword);
        long[] param = NumberUtils.getPageAndSize(page, size, allUserCount);
        List<User> userList = userMapper.searchUser(keyword, param[0], param[1]);
        fillPower(userList);
        PaginationDto<User> paginationDto = new PaginationDto<>();
        paginationDto.setData(userList);
        paginationDto.setPagination(param[2], param[3], param[1]);

        paginationDto.setAllCount(allUserCount);
        return paginationDto;
    }

    @Override
    public PaginationDto<User> getUserList(String page, String size) {
        long allUserCount = userMapper.selectCountByQuery(QueryWrapper.create());
        long[] param = NumberUtils.getPageAndSize(page, size, allUserCount);
        List<User> userList = userMapper.selectListByQuery(QueryWrapper.create()
                .orderBy(USER.ID.desc()).limit(param[0], param[1]));
        fillPower(userList);
        PaginationDto<User> paginationDto = new PaginationDto<>();
        paginationDto.setData(userList);
        paginationDto.setPagination(param[2], param[3], param[1]);

        paginationDto.setAllCount(allUserCount);
        return paginationDto;
    }

    /** 批量装配权限、角色并剥离密码，替代逐用户查询 */
    private void fillPower(List<User> userList) {
        if (userList.isEmpty()) {
            return;
        }
        Map<Long, UserPermission> permissionMap = userPermissionMapper
                .selectListByIds(userList.stream().map(User::getId).toList())
                .stream().collect(Collectors.toMap(UserPermission::getId, Function.identity()));
        for (User user : userList) {
            UserPermission permission = permissionMap.get(user.getId());
            if (permission != null) {
                user.setPower(permission.getPower());
            }
            user.clean();
        }
        rbacService.fillUserRoles(userList);
        userBanService.fillBanStatus(userList);
    }

    private PaginationDto<User> emptyPage(String page, String size) {
        long[] param = NumberUtils.getPageAndSize(page, size, 0);
        PaginationDto<User> paginationDto = new PaginationDto<>();
        paginationDto.setData(List.of());
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(0);
        return paginationDto;
    }

    @Override
    public long getAlluserCount() {
        return userMapper.selectCountByQuery(QueryWrapper.create());
    }
}
