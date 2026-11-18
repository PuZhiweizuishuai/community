package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.SysRoleMapper;
import com.buguagaoshu.community.mapper.SysUserRoleMapper;
import com.buguagaoshu.community.mapper.UserBanLogMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.SysRole;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserBanLog;
import com.buguagaoshu.community.model.UserBanStatus;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserBanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户封禁实现。
 *
 * <p>封禁状态同时写入 users（便于快速判断）与 user_ban_log（便于追溯）。
 * 状态查询带 30 秒本地缓存，封禁/解封时主动失效；临时封禁到期后按 endTime 实时判定。
 */
@Slf4j
@Service
public class UserBanServiceImpl implements UserBanService {

    public static final int TYPE_TEMPORARY = 1;
    public static final int TYPE_PERMANENT = 2;

    private static final long CACHE_TTL_MILLIS = 30_000L;
    private static final int MAX_REASON_LENGTH = 255;
    private static final int MAX_BAN_DAYS = 3650;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private record CachedStatus(UserBanStatus status, long expireAt) {
    }

    private final UserMapper userMapper;
    private final UserBanLogMapper userBanLogMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final OnlineUserService onlineUserService;
    private final Map<Long, CachedStatus> cache = new ConcurrentHashMap<>();

    public UserBanServiceImpl(UserMapper userMapper,
                              UserBanLogMapper userBanLogMapper,
                              SysUserRoleMapper sysUserRoleMapper,
                              SysRoleMapper sysRoleMapper,
                              OnlineUserService onlineUserService) {
        this.userMapper = userMapper;
        this.userBanLogMapper = userBanLogMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.onlineUserService = onlineUserService;
    }

    @Override
    public boolean isBanned(long userId) {
        return isActive(statusOf(userId), System.currentTimeMillis());
    }

    @Override
    public String bannedMessage(long userId) {
        UserBanStatus status = statusOf(userId);
        return isActive(status, System.currentTimeMillis()) ? formatMessage(status) : "";
    }

    @Override
    public Map<String, Object> banInfo(long userId) {
        UserBanStatus status = statusOf(userId);
        boolean banned = isActive(status, System.currentTimeMillis());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("banned", banned);
        data.put("banType", banned ? status.getBanType() : 0);
        data.put("banEndTime", banned ? status.getBanEndTime() : 0);
        data.put("banReason", banned ? status.getBanReason() : null);
        data.put("banMessage", banned ? formatMessage(status) : null);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ban(long userId, BanCommand command, User operator) {
        if (command == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        int type = command.type();
        if (type != TYPE_TEMPORARY && type != TYPE_PERMANENT) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "封禁类型不正确");
        }
        long now = System.currentTimeMillis();
        long endTime = 0L;
        if (type == TYPE_TEMPORARY) {
            Integer days = command.days();
            if (days == null || days < 1 || days > MAX_BAN_DAYS) {
                throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                        "临时封禁天数需在 1 ~ " + MAX_BAN_DAYS + " 天之间");
            }
            endTime = now + days * 24L * 3600L * 1000L;
        }
        String reason = StringUtils.trimToEmpty(command.reason());
        if (reason.isEmpty()) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写封禁原因");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                    "封禁原因不能超过 " + MAX_REASON_LENGTH + " 个字符");
        }
        User target = userMapper.selectOneById(userId);
        if (target == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (operator != null && operator.getId() == userId) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(), "不能封禁自己");
        }
        ensureNotSuperAdmin(userId);

        long operatorId = operator == null ? 0L : operator.getId();
        userMapper.updateBanStatus(userId, type, endTime, reason, now, operatorId);

        // 关闭旧的生效记录，再写入新的封禁流水
        userBanLogMapper.liftActive(userId, now, operatorId);
        UserBanLog banLog = new UserBanLog();
        banLog.setUserId(userId);
        banLog.setBanType(type);
        banLog.setEndTime(endTime);
        banLog.setReason(reason);
        banLog.setOperatorId(operatorId);
        banLog.setCreateTime(now);
        banLog.setStatus(1);
        userBanLogMapper.insert(banLog);

        // 吊销「记住我」token，避免旧 token 在封禁期间恢复登录
        onlineUserService.deleteOnlineUserById(userId);
        evict(userId);
        log.info("管理员 {} 封禁用户 {}，类型 {}，原因：{}", operatorId, userId, type, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unban(long userId, User operator) {
        User target = userMapper.selectOneById(userId);
        if (target == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        long now = System.currentTimeMillis();
        long operatorId = operator == null ? 0L : operator.getId();
        userMapper.updateBanStatus(userId, 0, 0L, null, 0L, 0L);
        userBanLogMapper.liftActive(userId, now, operatorId);
        evict(userId);
        log.info("管理员 {} 解封用户 {}", operatorId, userId);
    }

    @Override
    public void fillBanStatus(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(User::getId).filter(id -> id > 0).toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, UserBanStatus> statusMap = loadStatuses(userIds);
        long now = System.currentTimeMillis();
        for (User user : users) {
            UserBanStatus status = statusMap.get(user.getId());
            if (status == null || !isActive(status, now)) {
                user.setBanType(0);
                user.setBanEndTime(0L);
                user.setBanReason(null);
            } else {
                user.setBanType(status.getBanType());
                user.setBanEndTime(status.getBanEndTime());
                user.setBanReason(status.getBanReason());
            }
        }
    }

    private UserBanStatus statusOf(long userId) {
        long now = System.currentTimeMillis();
        CachedStatus cached = cache.get(userId);
        if (cached != null && cached.expireAt() > now) {
            return cached.status();
        }
        UserBanStatus status = loadStatus(userId);
        cache.put(userId, new CachedStatus(status, now + CACHE_TTL_MILLIS));
        return status;
    }

    private UserBanStatus loadStatus(long userId) {
        UserBanStatus status = userMapper.selectBanStatus(userId);
        if (status == null) {
            status = new UserBanStatus();
            status.setUserId(userId);
        }
        return status;
    }

    private Map<Long, UserBanStatus> loadStatuses(Collection<Long> userIds) {
        return userMapper.selectBanStatusByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserBanStatus::getUserId, Function.identity(), (a, b) -> a));
    }

    private boolean isActive(UserBanStatus status, long now) {
        if (status == null) {
            return false;
        }
        if (status.getBanType() == TYPE_PERMANENT) {
            return true;
        }
        return status.getBanType() == TYPE_TEMPORARY && status.getBanEndTime() > now;
    }

    private String formatMessage(UserBanStatus status) {
        String reason = StringUtils.isBlank(status.getBanReason()) ? "" : "，原因：" + status.getBanReason();
        if (status.getBanType() == TYPE_PERMANENT) {
            return "账号已被永久封禁" + reason;
        }
        return "账号已被封禁至 " + TIME_FORMATTER.format(Instant.ofEpochMilli(status.getBanEndTime())) + reason;
    }

    private void ensureNotSuperAdmin(long userId) {
        SysRole superAdmin = sysRoleMapper.selectByCode("super_admin");
        if (superAdmin != null && sysUserRoleMapper.selectRoleIdsByUserId(userId).contains(superAdmin.getId())) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(), "不能封禁超级管理员");
        }
    }

    private void evict(long userId) {
        cache.remove(userId);
    }
}
