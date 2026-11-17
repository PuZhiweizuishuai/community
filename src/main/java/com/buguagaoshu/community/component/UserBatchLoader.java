package com.buguagaoshu.community.component;

import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.mapper.UserPermissionMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 列表页作者信息批量装配：一次 IN 查询取用户（可选权限），替代逐条 select 的 N+1。
 * 返回的 User 已剥离密码。
 */
@Component
public class UserBatchLoader {

    private final UserMapper userMapper;
    private final UserPermissionMapper userPermissionMapper;

    public UserBatchLoader(UserMapper userMapper, UserPermissionMapper userPermissionMapper) {
        this.userMapper = userMapper;
        this.userPermissionMapper = userPermissionMapper;
    }

    public Map<Long, User> loadUsers(Collection<Long> ids) {
        return loadUsers(ids, false);
    }

    public Map<Long, User> loadUsers(Collection<Long> ids, boolean withPower) {
        Set<Long> distinct = ids == null ? Set.of()
                : ids.stream().filter(Objects::nonNull).filter(id -> id > 0).collect(Collectors.toSet());
        if (distinct.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectListByIds(distinct);
        Map<Long, User> userMap = new HashMap<>(users.size());
        for (User user : users) {
            user.clean();
            userMap.put(user.getId(), user);
        }
        if (withPower) {
            Map<Long, UserPermission> permissionMap = userPermissionMapper.selectListByIds(distinct)
                    .stream().collect(Collectors.toMap(UserPermission::getId, Function.identity()));
            userMap.forEach((id, user) -> {
                UserPermission permission = permissionMap.get(id);
                if (permission != null) {
                    user.setPower(permission.getPower());
                }
            });
        }
        return userMap;
    }
}