package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.SysRoleMapper;
import com.buguagaoshu.community.mapper.SysUserRoleMapper;
import com.buguagaoshu.community.mapper.UserBanLogMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.SysRole;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserBanLog;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserBanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserBanServiceImplTest {

    private UserMapper userMapper;
    private UserBanLogMapper userBanLogMapper;
    private SysUserRoleMapper sysUserRoleMapper;
    private SysRoleMapper sysRoleMapper;
    private OnlineUserService onlineUserService;
    private UserBanServiceImpl userBanService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userBanLogMapper = mock(UserBanLogMapper.class);
        sysUserRoleMapper = mock(SysUserRoleMapper.class);
        sysRoleMapper = mock(SysRoleMapper.class);
        onlineUserService = mock(OnlineUserService.class);
        userBanService = new UserBanServiceImpl(userMapper, userBanLogMapper, sysUserRoleMapper,
                sysRoleMapper, onlineUserService);
        User target = new User();
        target.setId(7);
        when(userMapper.selectOneById(7L)).thenReturn(target);
        when(userMapper.updateBanStatus(eq(7L), eq(1), anyLong(), eq("刷屏"), anyLong(), eq(99L)))
                .thenReturn(1);
    }

    @Test
    void temporaryBanWritesEndTimeAndLog() {
        User operator = new User();
        operator.setId(99);
        long before = System.currentTimeMillis();

        userBanService.ban(7, new UserBanService.BanCommand(1, 3, "刷屏"), operator);

        ArgumentCaptor<Long> endTime = ArgumentCaptor.forClass(Long.class);
        verify(userMapper).updateBanStatus(eq(7L), eq(1), endTime.capture(), eq("刷屏"),
                anyLong(), eq(99L));
        long expectedMin = before + 3 * 24L * 3600L * 1000L;
        assertTrue(endTime.getValue() >= expectedMin && endTime.getValue() <= expectedMin + 5000);

        ArgumentCaptor<UserBanLog> log = ArgumentCaptor.forClass(UserBanLog.class);
        verify(userBanLogMapper).insert(log.capture());
        assertEquals(1, log.getValue().getBanType());
        assertEquals(3 * 24L * 3600L * 1000L,
                log.getValue().getEndTime() - log.getValue().getCreateTime());
        verify(onlineUserService).deleteOnlineUserById(7L);
    }

    @Test
    void cannotBanSelf() {
        User operator = new User();
        operator.setId(7);

        assertThrows(CustomizeException.class,
                () -> userBanService.ban(7, new UserBanService.BanCommand(2, null, "测试"), operator));
        verify(userMapper, never()).updateBanStatus(anyLong(), anyInt(), anyLong(),
                org.mockito.ArgumentMatchers.any(), anyLong(), anyLong());
    }

    @Test
    void cannotBanSuperAdmin() {
        SysRole superAdmin = new SysRole();
        superAdmin.setId(1);
        superAdmin.setCode("super_admin");
        when(sysRoleMapper.selectByCode("super_admin")).thenReturn(superAdmin);
        when(sysUserRoleMapper.selectRoleIdsByUserId(7L)).thenReturn(List.of(1L));
        User operator = new User();
        operator.setId(99);

        assertThrows(CustomizeException.class,
                () -> userBanService.ban(7, new UserBanService.BanCommand(2, null, "测试"), operator));
    }

    @Test
    void unbanClearsStatusAndLiftsActiveLog() {
        User operator = new User();
        operator.setId(99);

        userBanService.unban(7, operator);

        verify(userMapper).updateBanStatus(7L, 0, 0L, null, 0L, 0L);
        verify(userBanLogMapper).liftActive(eq(7L), anyLong(), eq(99L));
    }
}
