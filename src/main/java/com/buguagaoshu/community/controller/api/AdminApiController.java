package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.buguagaoshu.community.model.table.UserTableDef.USER;

/**
 * 后台登录 / 退出。
 * 保留 /admin/login 以兼容旧前端，新前端统一使用 /api/admin/login。
 */
@RestController
@Slf4j
public class AdminApiController {

    private final UserMapper userMapper;
    private final UserPermissionService userPermissionService;
    private final RateLimitService rateLimitService;

    public AdminApiController(UserMapper userMapper, UserPermissionService userPermissionService,
                              RateLimitService rateLimitService) {
        this.userMapper = userMapper;
        this.userPermissionService = userPermissionService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping({"/api/admin/login", "/admin/login"})
    public ResultDTO<Object> adminLogin(String email, String password, String validateCode,
                                        HttpServletRequest request) {
        if (!CaptchaUtil.ver(validateCode, request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        String limitKey = "adminLogin:" + (email == null ? "" : email.toLowerCase());
        if (rateLimitService.isLocked(limitKey)) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where(USER.EMAIL.eq(email)));
        if (user == null) {
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_EMAIL_ERROR);
        }
        if (!StringUtil.judgePassword(password, user.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_PASSWORD_ERROR);
        }
        UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
        if (userPermission == null || userPermission.getPower() != 0) {
            log.info("用户 {} 尝试访问后台，但权限不足", user.getId());
            return ResultDTO.errorOf(CustomizeErrorCode.FORBIDDEN);
        }
        rateLimitService.reset(limitKey);
        user.setPower(0);
        user.setPassword(null);
        request.getSession();
        request.changeSessionId();
        request.getSession().setAttribute(AuthUtil.SESSION_ADMIN, user);
        return ResultDTO.okOf();
    }

    @PostMapping({"/api/admin/logout", "/admin/signOut"})
    public ResultDTO<Object> adminLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(AuthUtil.SESSION_ADMIN);
        }
        return ResultDTO.okOf();
    }
}