package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
import com.wf.captcha.utils.CaptchaUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;

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

    @Autowired
    public SignUpApiController(UserService userService, UserPermissionService userPermissionService) {
        this.userService = userService;
        this.userPermissionService = userPermissionService;
    }


    /**
     * 注册新用户
     */
    @PostMapping("/api/register")
    @ResponseBody
    public HashMap<String, Object> userSignUp(@Valid User user, String validateCode, HttpServletRequest request) {
        if (!CaptchaUtil.ver(validateCode, request)) {
            CaptchaUtil.clear(request);
            return StringUtil.dealResultMessage(false, "验证码错误！");
        }

        if (StringUtils.isBlank(user.getUserId()) || !StringUtil.checkUserId(user.getUserId())) {
            return StringUtil.dealResultMessage(false, "用户ID不能为空！或用户ID格式不正确!");
        }
        if (StringUtils.isBlank(user.getUserName()) || user.getUserName().length() > 20) {
            return StringUtil.dealResultMessage(false, "昵称不能为空！或昵称超出规定长度!");
        }
        if (StringUtils.isBlank(user.getEmail()) || !StringUtil.checkEmail(user.getEmail())) {
            return StringUtil.dealResultMessage(false, "邮箱不能为空。或邮箱格式不正确");
        }
        if (StringUtils.isBlank(user.getPassword()) || !StringUtil.checkPassword(user.getPassword())) {
            return StringUtil.dealResultMessage(false, "密码不能为空！或密码格式不正确");
        }
        if (user.getBirthday() != null || !user.getBirthday().equals("")) {
            int age = StringUtil.getAge(user.getBirthday());
            if (age == -1) {
                return StringUtil.dealResultMessage(false, "生日格式不正确");
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
            return StringUtil.dealResultMessage(true, "注册成功！");
        } else if (result == -1) {
            return StringUtil.dealResultMessage(false, "该邮箱已被注册，请重新输入，，或登陆");
        } else if (result == -2) {
            return StringUtil.dealResultMessage(false, "该账号已被注册，请跟换账号");
        } else {
            return StringUtil.dealResultMessage(false, "系统异常，请稍后重试!");
        }
    }


    /**
     * 更新邮箱
     */
    @PatchMapping("/api/update/Email/{id}")
    public HashMap<String, Object> updateEmailById(@PathVariable("id") long id, String email, String password) {
        User user = userService.selectUserById(id);
        if (user == null) {
            return StringUtil.dealResultMessage(false, "用户不存在！");
        }
        if (StringUtil.judgePassword(password, user.getPassword())) {
            if (email.equals(user.getEmail())) {
                return StringUtil.dealResultMessage(false, "与旧邮箱相同，不需要修改！");
            }
            if (StringUtil.checkEmail(email)) {
                int result = userService.updateUserEmailById(id, email);
                if (result == 1) {
                    return StringUtil.dealResultMessage(true, "修改成功！");
                } else {
                    return StringUtil.dealResultMessage(false, "邮箱重复！请更换邮箱后重试重试。");
                }
            } else {
                return StringUtil.dealResultMessage(false, "邮箱格式错误！");
            }
        } else {
            return StringUtil.dealResultMessage(false, "密码错误！");
        }
    }


    /**
     * 更新用户名
     */
    @PatchMapping("/api/update/userName/{id}")
    public HashMap<String, Object> updateUserName(@PathVariable("id") long id, String newName, String password) {
        if (newName.length() > 20) {
            return StringUtil.dealResultMessage(false, "用户名过长！超过了20个字符！");
        }
        User user = userService.selectUserById(id);
        if (user != null) {
            if (newName.equals(user.getUserName())) {
                return StringUtil.dealResultMessage(false, "与旧用户名相同，无需修改!");
            }
            if (StringUtil.judgePassword(password, user.getPassword())) {
                int result = userService.updateUserNameById(id, newName);
                if (result == 1) {
                    return StringUtil.dealResultMessage(true, "修改成功！");
                } else {
                    return StringUtil.dealResultMessage(false, "修改失败，请重试");
                }
            } else {
                return StringUtil.dealResultMessage(false, "密码错误！");
            }
        } else {
            return StringUtil.dealResultMessage(false, "用户不存在");
        }

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
