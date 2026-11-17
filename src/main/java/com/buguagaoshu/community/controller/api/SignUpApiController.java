package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.component.RateLimitService;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.EmailTokenService;
import com.buguagaoshu.community.service.MailService;
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

    @Autowired
    public SignUpApiController(UserService userService, UserPermissionService userPermissionService,
                               RateLimitService rateLimitService, MailService mailService,
                               EmailTokenService emailTokenService, SystemConfigService systemConfigService) {
        this.userService = userService;
        this.userPermissionService = userPermissionService;
        this.rateLimitService = rateLimitService;
        this.mailService = mailService;
        this.emailTokenService = emailTokenService;
        this.systemConfigService = systemConfigService;
    }


    /**
     * 注册新用户。邮件服务启用时额外要求邮箱验证码（mailCode）。
     */
    @PostMapping("/api/register")
    public ResultDTO<Object> userSignUp(User user, String validateCode, String mailCode, HttpServletRequest request) {
        if (!systemConfigService.isRegisterEnabled()) {
            return ResultDTO.errorOf(CustomizeErrorCode.FORBIDDEN.getCode(), "站点已关闭新用户注册");
        }
        if (!rateLimitService.tryAcquire("register:" + IpUtil.getIpAddr(request), 5, Duration.ofHours(1))) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS.getCode(), "注册过于频繁，请 1 小时后再试！");
        }
        if (!CaptchaUtil.ver(validateCode, request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_CAPTCHA_ERROR);
        }

        if (StringUtils.isBlank(user.getUserId()) || !StringUtil.checkUserId(user.getUserId())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "用户ID不能为空！或用户ID格式不正确!");
        }
        if (StringUtils.isBlank(user.getUserName()) || user.getUserName().length() > 20) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "昵称不能为空！或昵称超出规定长度!");
        }
        if (StringUtils.isBlank(user.getEmail()) || !StringUtil.checkEmail(user.getEmail())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "邮箱不能为空。或邮箱格式不正确");
        }
        if (StringUtils.isBlank(user.getPassword()) || !StringUtil.checkPassword(user.getPassword())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "密码不能为空！或密码格式不正确");
        }
        // 邮件服务启用时校验邮箱验证码（校验即销毁）
        if (mailService.isEnabled() && !emailTokenService.verifyRegisterCode(user.getEmail(), mailCode)) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "邮箱验证码错误或已过期，请重新获取");
        }
        if (StringUtils.isNotBlank(user.getBirthday())) {
            int age = StringUtil.getAge(user.getBirthday());
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
        // 插入数据
        if (result == 1) {
            // 写入权限信息
            UserPermission userPermission = new UserPermission(user.getId(), 1, "0", StringUtil.getNowTime(), System.currentTimeMillis());
            userPermissionService.insertUserPermission(userPermission);
            return ResultDTO.okOf();
        } else if (result == -1) {
            return ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), "该邮箱已被注册，请重新输入，或登陆");
        } else if (result == -2) {
            return ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), "该账号已被注册，请更换账号");
        }
        return ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR);
    }


    /**
     * 更新邮箱：目标用户一律取自当前会话，需验证码 + 密码确认，失败计数锁定防爆破。
     */
    @PatchMapping("/api/update/email")
    public ResultDTO<Object> updateEmail(String email, String password, String validateCode,
                                         HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        if (!CaptchaUtil.ver(validateCode, request)) {
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
        if (!StringUtil.judgePassword(password, fresh.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_PASSWORD_ERROR);
        }
        rateLimitService.reset(limitKey);
        if (email == null || email.equals(fresh.getEmail())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "与旧邮箱相同，不需要修改！");
        }
        if (!StringUtil.checkEmail(email)) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "邮箱格式错误！");
        }
        int result = userService.updateUserEmailById(user.getId(), email);
        if (result == 1) {
            return ResultDTO.okOf();
        }
        return ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), "邮箱重复！请更换邮箱后重试。");
    }


    /**
     * 更新用户名：目标用户一律取自当前会话，需验证码 + 密码确认，失败计数锁定防爆破。
     */
    @PatchMapping("/api/update/userName")
    public ResultDTO<Object> updateUserName(String newName, String password, String validateCode,
                                            HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        if (!CaptchaUtil.ver(validateCode, request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }
        if (StringUtils.isBlank(newName) || newName.length() > 20) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "用户名不能为空且不能超过 20 个字符！");
        }
        String limitKey = "updateUserName:" + user.getId();
        if (rateLimitService.isLocked(limitKey)) {
            return ResultDTO.errorOf(CustomizeErrorCode.TOO_MANY_ATTEMPTS);
        }
        User fresh = userService.selectUserById(user.getId());
        if (fresh == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        if (newName.equals(fresh.getUserName())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "与旧用户名相同，无需修改!");
        }
        if (!StringUtil.judgePassword(password, fresh.getPassword())) {
            rateLimitService.recordFailure(limitKey);
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_PASSWORD_ERROR);
        }
        rateLimitService.reset(limitKey);
        int result = userService.updateUserNameById(user.getId(), newName);
        if (result == 1) {
            return ResultDTO.okOf();
        }
        return ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR.getCode(), "修改失败，请重试");
    }


    /**
     * 补全默认头像信息
     */
    private void setDefaultHeadUrl(User user) {
        if (user.getSex() == null || user.getSex().equals("")) {
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