package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.PermissionChecker;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.SysPermissionMapper;
import com.buguagaoshu.community.mapper.SysRoleMapper;
import com.buguagaoshu.community.mapper.SysRolePermissionMapper;
import com.buguagaoshu.community.mapper.SysUserRoleMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.SysPermission;
import com.buguagaoshu.community.model.SysRole;
import com.buguagaoshu.community.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacServiceImplTest {

    private SysRoleMapper sysRoleMapper;
    private SysPermissionMapper sysPermissionMapper;
    private SysRolePermissionMapper sysRolePermissionMapper;
    private SysUserRoleMapper sysUserRoleMapper;
    private UserMapper userMapper;
    private PermissionChecker permissionChecker;
    private RbacServiceImpl rbacService;

    @BeforeEach
    void setUp() {
        sysRoleMapper = mock(SysRoleMapper.class);
        sysPermissionMapper = mock(SysPermissionMapper.class);
        sysRolePermissionMapper = mock(SysRolePermissionMapper.class);
        sysUserRoleMapper = mock(SysUserRoleMapper.class);
        userMapper = mock(UserMapper.class);
        permissionChecker = mock(PermissionChecker.class);
        rbacService = new RbacServiceImpl(sysRoleMapper, sysPermissionMapper, sysRolePermissionMapper,
                sysUserRoleMapper, userMapper, permissionChecker);
    }

    @Test
    void savingRolePermissionsAlwaysKeepsAdminAccess() {
        SysRole role = role(5L, false);
        when(sysRoleMapper.selectOneById(5L)).thenReturn(role);
        SysPermission userPermission = permission(9L, "admin:user");
        SysPermission accessPermission = permission(1L, "admin:access");
        when(sysPermissionMapper.selectListByIds(any())).thenReturn(List.of(userPermission));
        when(sysPermissionMapper.selectByCode("admin:access")).thenReturn(accessPermission);

        rbacService.saveRolePermissions(5L, List.of(9L));

        verify(sysRolePermissionMapper).deleteByRoleId(5L);
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(sysRolePermissionMapper).insertIgnoreBatch(eq(5L), ids.capture());
        assertEquals(2, ids.getValue().size());
        assertTrue(ids.getValue().containsAll(List.of(9L, 1L)));
        verify(permissionChecker).evictAll();
    }

    @Test
    void communityPermissionsDoNotGrantAdminAccess() {
        SysRole role = role(6L, true);
        role.setCode("user");
        when(sysRoleMapper.selectOneById(6L)).thenReturn(role);
        SysPermission comment = permission(15L, "content:comment:create");
        when(sysPermissionMapper.selectListByIds(any())).thenReturn(List.of(comment));

        rbacService.saveRolePermissions(6L, List.of(15L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(sysRolePermissionMapper).insertIgnoreBatch(eq(6L), ids.capture());
        assertEquals(List.of(15L), List.copyOf(ids.getValue()));
        verify(sysRolePermissionMapper, never()).insertIgnore(6L, 1L);
        verify(sysPermissionMapper, never()).selectByCode("admin:access");
    }

    @Test
    void superAdminPermissionsCannotBeModified() {
        SysRole role = role(1L, true);
        role.setCode("super_admin");
        when(sysRoleMapper.selectOneById(1L)).thenReturn(role);

        assertThrows(CustomizeException.class, () -> rbacService.saveRolePermissions(1L, List.of(9L)));

        verify(sysRolePermissionMapper, never()).deleteByRoleId(anyLong());
    }

    @Test
    void builtInRoleCannotBeDeleted() {
        when(sysRoleMapper.selectOneById(1L)).thenReturn(role(1L, true));

        assertThrows(CustomizeException.class, () -> rbacService.deleteRole(1L));

        verify(sysRoleMapper, never()).deleteById(anyLong());
    }

    @Test
    void lastSuperAdminCannotBeRemoved() {
        User user = new User();
        user.setId(7L);
        when(userMapper.selectOneById(7L)).thenReturn(user);
        SysRole superAdmin = new SysRole();
        superAdmin.setId(2L);
        superAdmin.setCode("super_admin");
        superAdmin.setBuiltIn(1);
        when(sysRoleMapper.selectByCode("super_admin")).thenReturn(superAdmin);
        when(sysUserRoleMapper.selectRoleIdsByUserId(7L)).thenReturn(List.of(2L));
        when(sysUserRoleMapper.countUsersByRoleId(2L)).thenReturn(1L);

        assertThrows(CustomizeException.class, () -> rbacService.saveUserRoles(7L, List.of()));

        verify(sysUserRoleMapper, never()).deleteByUserId(anyLong());
    }

    private SysRole role(long id, boolean builtIn) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setName("测试角色");
        role.setCode("test_role");
        role.setBuiltIn(builtIn ? 1 : 0);
        role.setStatus(1);
        return role;
    }

    private SysPermission permission(long id, String code) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setCode(code);
        return permission;
    }
}
