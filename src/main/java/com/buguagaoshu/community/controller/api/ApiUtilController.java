package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.IpUtil;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 15:22
 * 一些工具栏 api 接口
 */
@RestController
public class ApiUtilController {
    private final UserService userService;

    @Autowired
    public ApiUtilController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 返回该用户当前访问的 ip
     */
    @GetMapping("/api/getUserIp")
    public ResultDTO<String> getUserIP(HttpServletRequest request) {
        return ResultDTO.okOf(IpUtil.getIpAddr(request));
    }


    /**
     * 检查邮箱是否可注册：data.available=false 时 data.message 为原因
     */
    @GetMapping("/api/checkEmail")
    public ResultDTO<Map<String, Object>> checkEmail(String email) {
        if (email == null || !StringUtil.checkEmail(email)) {
            return ResultDTO.okOf(Map.of("available", false, "message", "该邮箱格式错误！"));
        }
        if (userService.selectUserByEmail(email) != null) {
            return ResultDTO.okOf(Map.of("available", false, "message", "该邮箱已被注册，请重新输入，或登陆"));
        }
        return ResultDTO.okOf(Map.of("available", true, "message", "该邮箱可用"));
    }


    /**
     * 检查账号是否可注册
     */
    @GetMapping("/api/checkUserId")
    public ResultDTO<Map<String, Object>> checkUserId(String userId) {
        if (userId == null || !StringUtil.checkUserId(userId)) {
            return ResultDTO.okOf(Map.of("available", false, "message", "该账号格式错误！"));
        }
        if (userService.selectUserByUserId(userId) != null) {
            return ResultDTO.okOf(Map.of("available", false, "message", "该账号已被注册，请重新输入,或更换账号！"));
        }
        return ResultDTO.okOf(Map.of("available", true, "message", "该账号可用"));
    }
}