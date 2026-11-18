package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.PermissionChecker;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.SysPermissionMapper;
import com.buguagaoshu.community.mapper.SysRoleMapper;
import com.buguagaoshu.community.mapper.SysRolePermissionMapper;
import com.buguagaoshu.community.mapper.SysUserRoleMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.SysPermission;
import com.buguagaoshu.community.model.SysRole;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserRoleRow;
import com.buguagaoshu.community.service.RbacService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RBAC 角色与权限实现。
 *
 * <p>约定：
 * <ul>
 *   <li>{@code super_admin} 为内置角色：不可删除、权限不可编辑，保证系统始终有超级管理员；</li>
 *   <li>角色一旦拥有任意权限，必须同时拥有 {@code admin:access}，否则后台接口无法访问；</li>
 *   <li>权限 / 角色变更后立即清空权限缓存，避免最长 30 秒的旧权限窗口。</li>
 * </ul>
 */
@Slf4j
@Service
public class RbacServiceImpl implements RbacService {

    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{2,31}$");
    private static final String SUPER_ADMIN_CODE = "super_admin";
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final UserMapper userMapper;
    private final PermissionChecker permissionChecker;

    public RbacServiceImpl(SysRoleMapper sysRoleMapper,
                           SysPermissionMapper sysPermissionMapper,
                           SysRolePermissionMapper sysRolePermissionMapper,
                           SysUserRoleMapper sysUserRoleMapper,
                           UserMapper userMapper,
                           PermissionChecker permissionChecker) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.userMapper = userMapper;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public List<SysRole> listRoles() {
        return sysRoleMapper.selectAll().stream()
                .sorted(Comparator.comparingInt(SysRole::getSortOrder).thenComparingLong(SysRole::getId))
                .toList();
    }

    @Override
    public List<Map<String, Object>> listRoleViews() {
        Map<Long, Long> permissionCounts = toCountMap(sysRolePermissionMapper.countPermissionsByRole());
        Map<Long, Long> userCounts = toCountMap(sysUserRoleMapper.countUsersByRole());
        List<Map<String, Object>> views = new ArrayList<>();
        for (SysRole role : listRoles()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", role.getId());
            view.put("code", role.getCode());
            view.put("name", role.getName());
            view.put("description", role.getDescription());
            view.put("builtIn", role.getBuiltIn() == 1);
            view.put("status", role.getStatus());
            view.put("sortOrder", role.getSortOrder());
            view.put("permissionCount", permissionCounts.getOrDefault(role.getId(), 0L));
            view.put("userCount", userCounts.getOrDefault(role.getId(), 0L));
            view.put("createTime", role.getCreateTime());
            view.put("updateTime", role.getUpdateTime());
            views.add(view);
        }
        return views;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole saveRole(RoleSaveCommand command) {
        if (command == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        String name = StringUtils.trimToEmpty(command.name());
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                    "角色名称不能为空且不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }
        String description = StringUtils.trimToEmpty(command.description());
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                    "角色说明不能超过 " + MAX_DESCRIPTION_LENGTH + " 个字符");
        }
        long now = System.currentTimeMillis();
        SysRole sameName = sysRoleMapper.selectByName(name);
        boolean isNew = command.id() == null || command.id() <= 0;
        if (sameName != null && (isNew || sameName.getId() != command.id())) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "角色名称「" + name + "」已存在");
        }

        if (isNew) {
            String code = StringUtils.trimToEmpty(command.code()).toLowerCase();
            if (!ROLE_CODE_PATTERN.matcher(code).matches()) {
                throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                        "角色编码需以小写字母开头，仅包含小写字母、数字和下划线，长度 3~32");
            }
            if (sysRoleMapper.selectByCode(code) != null) {
                throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "角色编码「" + code + "」已存在");
            }
            SysRole role = new SysRole();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            role.setBuiltIn(0);
            role.setStatus(command.status() != null && command.status() == 0 ? 0 : 1);
            role.setSortOrder(command.sortOrder() == null ? 0 : command.sortOrder());
            role.setCreateTime(now);
            role.setUpdateTime(now);
            sysRoleMapper.insert(role);
            log.info("新增角色 {}（{}），角色ID {}", name, code, role.getId());
            return role;
        }

        SysRole role = sysRoleMapper.selectOneById(command.id());
        if (role == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        role.setName(name);
        role.setDescription(description);
        role.setSortOrder(command.sortOrder() == null ? role.getSortOrder() : command.sortOrder());
        if (role.getBuiltIn() == 1) {
            // 内置角色不允许停用，编码也不允许变更
            role.setStatus(1);
        } else {
            role.setStatus(command.status() != null && command.status() == 0 ? 0 : 1);
        }
        role.setUpdateTime(now);
        sysRoleMapper.update(role);
        permissionChecker.evictAll();
        log.info("更新角色 {}（{}），角色ID {}", name, role.getCode(), role.getId());
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(long roleId) {
        SysRole role = sysRoleMapper.selectOneById(roleId);
        if (role == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (role.getBuiltIn() == 1) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(), "内置角色不可删除");
        }
        sysRoleMapper.deleteById(roleId);
        permissionChecker.evictAll();
        log.info("删除角色 {}（{}），角色ID {}", role.getName(), role.getCode(), roleId);
    }

    @Override
    public List<SysPermission> listPermissions() {
        // 按全局 sortOrder 排序：分组首次出现的顺序即「基础 → 用户 → 内容 → 运营 → 系统」
        return sysPermissionMapper.selectAll().stream()
                .sorted(Comparator.comparingInt(SysPermission::getSortOrder)
                        .thenComparingLong(SysPermission::getId))
                .toList();
    }

    @Override
    public List<Long> listRolePermissionIds(long roleId) {
        return sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(long roleId, List<Long> permissionIds) {
        SysRole role = sysRoleMapper.selectOneById(roleId);
        if (role == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        // 仅超级管理员权限锁定；默认「社区成员」等内置角色允许管理员动态调整权限
        if (SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(), "超级管理员权限不可修改");
        }
        Set<Long> ids = distinct(permissionIds);
        if (!ids.isEmpty()) {
            List<SysPermission> permissions = sysPermissionMapper.selectListByIds(ids);
            if (permissions.size() != ids.size()) {
                throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "包含不存在的权限点");
            }
            // 仅当角色包含后台权限时，才需要补齐「后台访问」；社区权限（content/file）不要求后台身份
            boolean requiresAdminAccess = permissions.stream()
                    .anyMatch(permission -> permission.getCode() != null
                            && permission.getCode().startsWith("admin:"));
            if (requiresAdminAccess) {
                SysPermission access = sysPermissionMapper.selectByCode(PermissionCodes.ADMIN_ACCESS);
                if (access != null) {
                    ids.add(access.getId());
                }
            }
        }
        sysRolePermissionMapper.deleteByRoleId(roleId);
        for (Long permissionId : ids) {
            sysRolePermissionMapper.insertIgnore(roleId, permissionId);
        }
        permissionChecker.evictAll();
        log.info("更新角色 {} 的权限，共 {} 个权限点", role.getName(), ids.size());
    }

    @Override
    public List<Long> listUserRoleIds(long userId) {
        return sysUserRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserRoles(long userId, List<Long> roleIds) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        Set<Long> ids = distinct(roleIds);
        if (!ids.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectListByIds(ids);
            if (roles.size() != ids.size()) {
                throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "包含不存在的角色");
            }
        }
        ensureSuperAdminRemains(userId, ids);
        sysUserRoleMapper.deleteByUserId(userId);
        for (Long roleId : ids) {
            sysUserRoleMapper.insertIgnore(userId, roleId);
        }
        permissionChecker.evict(userId);
        log.info("更新用户 {} 的角色，共 {} 个角色", userId, ids.size());
    }

    @Override
    public void assignRoleByCode(long userId, String roleCode) {
        SysRole role = sysRoleMapper.selectByCode(roleCode);
        if (role == null) {
            log.warn("分配默认角色失败：角色编码 {} 不存在", roleCode);
            return;
        }
        sysUserRoleMapper.insertIgnore(userId, role.getId());
        permissionChecker.evict(userId);
    }

    @Override
    public void fillUserRoles(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(User::getId).filter(id -> id > 0).toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, List<UserRoleRow>> byUser = sysUserRoleMapper.selectRoleNamesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleRow::getUserId, LinkedHashMap::new, Collectors.toList()));
        for (User user : users) {
            List<UserRoleRow> rows = byUser.getOrDefault(user.getId(), List.of());
            user.setRoleIds(rows.stream().map(UserRoleRow::getRoleId).toList());
            user.setRoleCodes(rows.stream().map(UserRoleRow::getRoleCode).toList());
            user.setRoleNames(rows.stream().map(UserRoleRow::getRoleName).toList());
        }
    }

    /** 至少保留一名超级管理员，避免把系统锁死在无人可管理后台的状态 */
    private void ensureSuperAdminRemains(long userId, Set<Long> newRoleIds) {
        SysRole superAdmin = sysRoleMapper.selectByCode(SUPER_ADMIN_CODE);
        if (superAdmin == null) {
            return;
        }
        boolean wasSuperAdmin = sysUserRoleMapper.selectRoleIdsByUserId(userId).contains(superAdmin.getId());
        if (!wasSuperAdmin || newRoleIds.contains(superAdmin.getId())) {
            return;
        }
        if (sysUserRoleMapper.countUsersByRoleId(superAdmin.getId()) <= 1) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN.getCode(),
                    "系统至少需要保留一名超级管理员，无法移除最后一个超级管理员角色");
        }
    }

    private Set<Long> distinct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return ids.stream().filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object roleId = row.get("roleId");
            Object total = row.get("total");
            if (roleId instanceof Number roleKey && total instanceof Number count) {
                result.put(roleKey.longValue(), count.longValue());
            }
        }
        return result;
    }
}
