package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.FollowUserMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.FollowUser;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.FollowUserService;
import com.buguagaoshu.community.util.NumberUtils;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.buguagaoshu.community.model.table.FollowUserTableDef.FOLLOW_USER;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 16:46
 */
@Service
public class FollowUserServiceImpl implements FollowUserService {
    private final FollowUserMapper followUserMapper;

    private final UserMapper userMapper;

    private final UserBatchLoader userBatchLoader;

    public FollowUserServiceImpl(FollowUserMapper followUserMapper, UserMapper userMapper,
                                 UserBatchLoader userBatchLoader) {
        this.followUserMapper = followUserMapper;
        this.userMapper = userMapper;
        this.userBatchLoader = userBatchLoader;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertFollowUser(long followUserId, long userId, User follow) {
        FollowUser followUser = new FollowUser();
        followUser.setFollowUserId(followUserId);
        followUser.setUserId(userId);
        followUser.setCreateTime(System.currentTimeMillis());
        FollowUser temp = findFollow(followUserId, userId);
        if (temp == null) {
            try {
                followUserMapper.insertSelective(followUser);
            } catch (DuplicateKeyException e) {
                // 并发重复关注：唯一索引兜底，不再累加计数
                return 1;
            }
            // 更新粉丝数
            follow.setFansCount(1);
            userMapper.updateUserFansCount(follow);
            // 更新关注数
            User user = new User();
            user.setId(userId);
            user.setFollowCount(1);
            userMapper.updateUserFollowCount(user);
            return 1;
        } else {
            int deleted = followUserMapper.deleteById(temp.getId());
            if (deleted == 1) {
                // 更新粉丝数
                follow.setFansCount(-1);
                userMapper.updateUserFansCount(follow);
                // 更新关注数
                User user = new User();
                user.setId(userId);
                user.setFollowCount(-1);
                userMapper.updateUserFollowCount(user);
            }
            return 2;
        }
    }

    @Override
    public boolean isFollowUser(FollowUser followUser) {
        return findFollow(followUser.getFollowUserId(), followUser.getUserId()) != null;
    }

    private FollowUser findFollow(long followUserId, long userId) {
        return followUserMapper.selectOneByQuery(QueryWrapper.create()
                .where(FOLLOW_USER.FOLLOW_USER_ID.eq(followUserId))
                .and(FOLLOW_USER.USER_ID.eq(userId)));
    }

    @Override
    public PaginationDto<User> getFollowUserList(long userId, String page, String size) {
        QueryWrapper query = QueryWrapper.create().where(FOLLOW_USER.USER_ID.eq(userId));
        long allCount = followUserMapper.selectCountByQuery(query);
        long[] param = NumberUtils.getPageAndSize(page, size, allCount);
        List<FollowUser> followUsers = followUserMapper.selectListByQuery(
                query.orderBy(FOLLOW_USER.ID.desc()).limit(param[0], param[1]));
        Map<Long, User> userMap = userBatchLoader.loadUsers(
                followUsers.stream().map(FollowUser::getFollowUserId).toList());
        List<User> userList = new ArrayList<>();
        for (FollowUser followUser : followUsers) {
            User user = userMap.get(followUser.getFollowUserId());
            if (user != null) {
                userList.add(user);
            }
        }
        PaginationDto<User> paginationDto = new PaginationDto<>();
        paginationDto.setData(userList);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allCount);
        return paginationDto;

    }

    @Override
    public PaginationDto<User> getFansList(long userId, String page, String size) {
        QueryWrapper query = QueryWrapper.create().where(FOLLOW_USER.FOLLOW_USER_ID.eq(userId));
        long allCount = followUserMapper.selectCountByQuery(query);
        long[] param = NumberUtils.getPageAndSize(page, size, allCount);
        List<FollowUser> followUsers = followUserMapper.selectListByQuery(
                query.orderBy(FOLLOW_USER.ID.desc()).limit(param[0], param[1]));
        Map<Long, User> userMap = userBatchLoader.loadUsers(
                followUsers.stream().map(FollowUser::getUserId).toList());
        List<User> userList = new ArrayList<>();
        for (FollowUser followUser : followUsers) {
            User user = userMap.get(followUser.getUserId());
            if (user != null) {
                userList.add(user);
            }
        }
        PaginationDto<User> paginationDto = new PaginationDto<>();
        paginationDto.setData(userList);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allCount);
        return paginationDto;
    }
}