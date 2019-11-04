package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.util.StringUtil;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 19:09
 */
@RestController
@Slf4j
public class AdminApiController {
    final
    UserMapper userMapper;

    final
    UserPermissionService userPermissionService;

    @Autowired
    public AdminApiController(UserMapper userMapper, UserPermissionService userPermissionService) {
        this.userMapper = userMapper;
        this.userPermissionService = userPermissionService;
    }


    @PostMapping("/admin/login")
    public Map<String, Object> adminLogin(String email, String password, String validateCode,
                                          HttpServletRequest request) {
        if (!CaptchaUtil.ver(validateCode, request)) {
            CaptchaUtil.clear(request);
            return StringUtil.dealResultMessage(false, "验证码错误！");
        }
        User user = userMapper.selectUserByEmail(email);
        if(user == null) {
            return StringUtil.dealResultMessage(false, "用户名不存在！");
        }

        if(StringUtil.judgePassword(password, user.getPassword())) {
            UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
            if(userPermission.getPower() != 0) {
                log.info("用户id为 {} 的用户尝试访问管理员页面，但因权限不足被打回！", user.getId());
                return StringUtil.dealResultMessage(false, "权限不足！");
            }
            user.setPower(userPermission.getPower());
            request.getSession().setAttribute("admin", user);
            return StringUtil.dealResultMessage(true, "登陆成功！");
        } else {
            return StringUtil.dealResultMessage(false, "密码错误！");
        }
    }
}
