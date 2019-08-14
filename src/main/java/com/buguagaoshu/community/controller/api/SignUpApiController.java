package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

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
    public HashMap<String, Object> userSignUp(@Valid User user, Errors errors) {
        // 校验数据并返回错误信息
        HashMap<String, Object> infoMap = new HashMap<>(2);
        List<ObjectError> oes = errors.getAllErrors();
        if (user.getUserName().isEmpty()) {
            infoMap.put("userName", "用户名不能为空！");
        }
        if (user.getEmail().isEmpty()) {
            infoMap.put("email", "email不能为空！");
        }
        if (oes.size() != 0) {
            for (ObjectError oe : oes) {
                String key = null;
                String msg = null;
                if (oe instanceof FieldError) {
                    FieldError fe = (FieldError) oe;
                    key = fe.getField();
                } else {
                    key = oe.getObjectName();
                }
                msg = oe.getDefaultMessage();
                infoMap.put(key, msg);
            }
            return infoMap;
        }
        int age = StringUtil.getAge(user.getBirthday());
        if (age == -1) {
            infoMap.put("birthday", "生日输入超出当前日期！");
        }
        if (infoMap.size() != 0) {
            return infoMap;
        }
        // 补全数据
        setDefaultHeadUrl(user);
        user.setCreationTime(StringUtil.getNowTime());
        user.setLastTime(StringUtil.getNowTime());
        user.setAge(age);

        // 插入数据
        if (userService.insertUser(user) == 1) {
            // 写入权限信息
            UserPermission userPermission = new UserPermission(user.getId(),1,"0", StringUtil.getNowTime());
            userPermissionService.insertUserPermission(userPermission);
            return StringUtil.dealResultMessage(true, "注册成功！");
        } else {
            return StringUtil.dealResultMessage(false, "该邮箱已被注册，请重新输入，，或登陆");
        }
    }

    /**
     * 检查邮箱
     */
    @GetMapping("/api/checkEmail")
    @ResponseBody
    public HashMap<String, Object> checkEmail(String email) {
        if(!StringUtil.checkEmail(email)) {
            return StringUtil.dealResultMessage(false, "该邮箱格式错误！");
        }
        if (userService.selectUserByEmail(email) != null) {
            return StringUtil.dealResultMessage(false, "该邮箱已被注册，请重新输入，或登陆");
        } else {
            return StringUtil.dealResultMessage(true, "该邮箱已可用");
        }
    }

    /**
     * 更新密码
     * */
    @PatchMapping("/api/update/password/{id}")
    public HashMap<String, Object> updatePassword(@PathVariable("id") long id, String oldPassword, String newPassword) {
        if (oldPassword.equals(newPassword)) {
            return StringUtil.dealResultMessage(false, "两次输入密码相同！");
        }
        if (StringUtil.checkPassword(newPassword)) {
            User user = userService.selectUserById(id);
            if (user != null) {
                if (StringUtil.judgePassword(oldPassword, user.getPassword())) {
                    if (userService.updateUserPasswordById(id, newPassword) == 1) {
                        return StringUtil.dealResultMessage(true, "修改成功！");
                    } else {
                        return StringUtil.dealResultMessage(false, "修改失败，请稍后重试！");
                    }

                } else {
                    return StringUtil.dealResultMessage(false, "原密码错误！");
                }
            } else {
                return StringUtil.dealResultMessage(false, "用户不存在！");
            }
        } else {
            return StringUtil.dealResultMessage(false, "密码格式错误！");
        }
    }


    /**
     * 更新头像
     */
    @PatchMapping("/api/update/HeadUrl/{id}")
    public HashMap<String, Object> updateHeadUrlById(@PathVariable("id") long id, String url) {
        int result = userService.updateUserHeadUrlById(id, url);
        if (result == 1) {
            return StringUtil.dealResultMessage(true, "修改成功！");
        } else {
            return StringUtil.dealResultMessage(false, "修改失败！请重试。");
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
     * */
    @PatchMapping("/api/update/userName/{id}")
    public HashMap<String, Object> updateUserName(@PathVariable("id") long id, String newName, String password) {
        if(newName.length() > 20) {
            return StringUtil.dealResultMessage(false, "用户名过长！超过了20个字符！");
        }
        User user = userService.selectUserById(id);
        if(user != null) {
            if (newName.equals(user.getUserName())) {
                return StringUtil.dealResultMessage(false, "与旧用户名相同，无需修改!");
            }
            if(StringUtil.judgePassword(password, user.getPassword())) {
                int result = userService.updateUserNameById(id, newName);
                if(result == 1) {
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
        if (user.getSex().equals("男")) {
            user.setHeadUrl("image/head/boyhead.png");
        } else if (user.getSex().equals("女")) {
            user.setHeadUrl("image/head/girlhead.png");
        } else {
            user.setHeadUrl("image/head/nohead.png");
        }
    }
}
