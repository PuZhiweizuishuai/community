package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.dto.RegisterRequest;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.UpdateEmailRequest;
import com.buguagaoshu.community.dto.UpdateUserNameRequest;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.EmailTokenService;
import com.buguagaoshu.community.service.MailService;
import com.buguagaoshu.community.service.RbacService;
import com.buguagaoshu.community.service.SystemConfigService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.StringUtil;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;

/**
 * 注册控制
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 16:29
 */
@RestController
public class SignUpApiController {
    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final RateLimitService rateLimitService;

    private final MailService mailService;

    private final EmailTokenService emailTokenService;

    private final SystemConfigService systemConfigService;

    private final RbacService rbacService;

    @Autowired
    public SignUpApiController(UserService userService, UserPermissionService userPermissionService,
                               RateLimitService rateLimitService, MailService mailService,
                               EmailTokenService emailTokenService, SystemConfigService systemConfigService,
                               RbacService rbacService) {
        this.userService = userService;
        this.userPermissionService = userPermissionService;
        this.rateLimitService = rateLimitService;
        this.mailService = mailService;
        this.emailTokenService = emailTokenService;
        this.systemConfigService = systemConfigService;
        this.rbacService = rbacService;
    }


    /**
     * 注册新用户。邮件服务启用时额外要求邮箱验证码（mailCode）。
     */
    @PostMapping("/api/register")
    public ResultDTO<Object> userSignUp(@Valid RegisterRequest body, HttpServletRequest request) {
        if (!systemConfigService.isRegisterEnabled()) {
            return ResultDTO.errorOf(CustomizeErrorCode.FORBIDDEN.getCode(), "站点已关闭新用户注册");
        }
        if (!rateLimitService.tryAcquire("register:" + IpUtil.getIpAddr(request), 5, Duration.ofHours(1))) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS.getCode(), "注册过于频繁，请 1 小时后再试！");
        }
        if (!CaptchaUtil.ver(body.getValidateCode(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }
        // 邮件服务启用时校验邮箱验证码（校验即销毁）
        if (mailService.isEnabled() && !emailTokenService.verifyRegisterCode(body.getEmail(), body.getMailCode())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "邮箱验证码错误或已过期，请重新获取");
        }

        User user = new User();
        user.setUserId(body.getUserId());
        user.setUserName(body.getUserName());
        user.setEmail(body.getEmail());
        user.setPassword(body.getPassword());
        user.setSex(body.getSex());
        user.setBirthday(body.getBirthday());
        user.setSchool(body.getSchool());
        user.setMajor(body.getMajor());
        if (StringUtils.isNotBlank(body.getBirthday())) {
            int age = StringUtil.getAge(body.getBirthday());
            if (age == -1) {
                return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "生日格式不正确");
            }
            user.setAge(age);
        }

        // 补全数据
        setDefaultHeadUrl(user);
        user.setUserTopPhotoUrl("/image/101-desktop-wallpaper.png");
        user.setCreationTime(StringUtil.getNowTime());
        user.setLastTime(StringUtil.getNowTime());

        int result = userService.insertUser(user);
        return switch (result) {
            case 1 -> {
                // 写入权限信息
                UserPermission userPermission = new UserPermission(user.getId(), 1, "0",
                        StringUtil.getNowTime(), System.currentTimeMillis());
                userPermissionService.insertUserPermission(userPermission);
                // 默认授予「社区成员」角色：发布问题、发表评论、上传文件
                rbacService.assignRoleByCode(user.getId(), RbacService.DEFAULT_ROLE_CODE);
                yield ResultDTO.okOf();
            }
            case -1 -> ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), "该邮箱已被注册，请重新输入，或登陆");
            case -2 -> ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), "该账号已被注册，请更换账号");
            default -> ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR);
        };
    }


    /**
     * 更新邮箱：目标用户一律取自当前会话，需验证码 + 密码确认，失败计数锁定防爆破。
     */
    @PatchMapping("/api/update/email")
    public ResultDTO<Object> updateEmail(@Valid UpdateEmailRequest body, HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        if (!CaptchaUtil.ver(body.getValidateCode(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }
        String limitKey = "updateEmail:" + user.getId();
        if (rateLimitService.isLocked(limitKey)) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        User fresh = userService.selectUserById(user.getId());
        if (fresh == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        if (!StringUtil.judgePassword(body.getPassword(), fresh.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "密码错误，请重试");
        }
        rateLimitService.reset(limitKey);
        if (body.getEmail().equals(fresh.getEmail())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "与旧邮箱相同，不需要修改！");
        }
        int result = userService.updateUserEmailById(user.getId(), body.getEmail());
        if (result == 1) {
            return ResultDTO.okOf();
        }
        return ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), "邮箱重复！请更换邮箱后重试。");
    }


    /**
     * 更新用户名：目标用户一律取自当前会话，需验证码 + 密码确认，失败计数锁定防爆破。
     */
    @PatchMapping("/api/update/userName")
    public ResultDTO<Object> updateUserName(@Valid UpdateUserNameRequest body, HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        if (!CaptchaUtil.ver(body.getValidateCode(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }
        String limitKey = "updateUserName:" + user.getId();
        if (rateLimitService.isLocked(limitKey)) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        User fresh = userService.selectUserById(user.getId());
        if (fresh == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        if (body.getNewName().equals(fresh.getUserName())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "与旧用户名相同，无需修改!");
        }
        if (!StringUtil.judgePassword(body.getPassword(), fresh.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "密码错误，请重试");
        }
        rateLimitService.reset(limitKey);
        int result = userService.updateUserNameById(user.getId(), body.getNewName());
        if (result == 1) {
            return ResultDTO.okOf();
        }
        return ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR.getCode(), "修改失败，请重试");
    }


    /**
     * 补全默认头像信息
     */
    private void setDefaultHeadUrl(User user) {
        if (StringUtils.isBlank(user.getSex())) {
            user.setSex("保密");
            user.setHeadUrl("/image/head/nohead.png");
            return;
        }
        if (user.getSex().equals("男")) {
            user.setHeadUrl("/image/head/boyhead.png");
        } else if (user.getSex().equals("女")) {
            user.setHeadUrl("/image/head/girlhead.png");
        } else {
            user.setHeadUrl("/image/head/nohead.png");
        }
    }
}