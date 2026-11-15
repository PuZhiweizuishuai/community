package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.captcha.CaptchaUtil;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台登录 / 退出。
 * 保留 /admin/login 以兼容旧前端，新前端统一使用 /api/admin/login。
 */
@RestController
@Slf4j
public class AdminApiController {

    private final UserMapper userMapper;
    private final UserPermissionService userPermissionService;

    public AdminApiController(UserMapper userMapper, UserPermissionService userPermissionService) {
        this.userMapper = userMapper;
        this.userPermissionService = userPermissionService;
    }

    @PostMapping({"/api/admin/login", "/admin/login"})
    public Map<String, Object> adminLogin(String email, String password, String validateCode,
                                          HttpServletRequest request) {
        if (!CaptchaUtil.ver(validateCode, request)) {
            CaptchaUtil.clear(request);
            return StringUtil.dealResultMessage(false, "验证码错误！");
        }
        CaptchaUtil.clear(request);
        User user = userMapper.selectUserByEmail(email);
        if (user == null) {
            return StringUtil.dealResultMessage(false, "用户名不存在！");
        }
        if (!StringUtil.judgePassword(password, user.getPassword())) {
            return StringUtil.dealResultMessage(false, "密码错误！");
        }
        UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
        if (userPermission == null || userPermission.getPower() != 0) {
            log.info("用户 {} 尝试访问后台，但权限不足", user.getId());
            return StringUtil.dealResultMessage(false, "权限不足！");
        }
        user.setPower(0);
        user.setPassword(null);
        request.getSession().setAttribute(AuthUtil.SESSION_ADMIN, user);
        return StringUtil.dealResultMessage(true, "登陆成功！");
    }

    @PostMapping({"/api/admin/logout", "/admin/signOut"})
    public Map<String, Object> adminLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(AuthUtil.SESSION_ADMIN);
        }
        return StringUtil.dealResultMessage(true, "已退出");
    }
}
