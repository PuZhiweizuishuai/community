package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.UserBanService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台用户封禁：查询封禁状态、临时/永久封禁、解封。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserBanApiController {

    /** type=1 临时封禁（days 天）/ type=2 永久封禁 */
    public record BanRequest(
            @NotNull(message = "封禁类型不能为空")
            @Min(value = 1, message = "封禁类型不正确")
            @Max(value = 2, message = "封禁类型不正确") Integer type,
            @Min(value = 1, message = "临时封禁天数需在 1 ~ 3650 天之间")
            @Max(value = 3650, message = "临时封禁天数需在 1 ~ 3650 天之间") Integer days,
            @Size(max = 255, message = "封禁原因不能超过 255 个字符") String reason) {
    }

    private final UserBanService userBanService;

    public AdminUserBanApiController(UserBanService userBanService) {
        this.userBanService = userBanService;
    }

    @GetMapping("/{id}/ban")
    @RequirePermission(PermissionCodes.ADMIN_USER_BAN)
    public ResultDTO<Map<String, Object>> status(@PathVariable("id") long id) {
        return ResultDTO.okOf(userBanService.banInfo(id));
    }

    @PostMapping("/{id}/ban")
    @RequirePermission(PermissionCodes.ADMIN_USER_BAN)
    public ResultDTO<Object> ban(@PathVariable("id") long id, @Valid @RequestBody BanRequest body,
                                 HttpServletRequest request) {
        User operator = AuthUtil.requireUser(request);
        userBanService.ban(id, new UserBanService.BanCommand(body.type(), body.days(), body.reason()), operator);
        return ResultDTO.okOf();
    }

    @PostMapping("/{id}/unban")
    @RequirePermission(PermissionCodes.ADMIN_USER_BAN)
    public ResultDTO<Object> unban(@PathVariable("id") long id, HttpServletRequest request) {
        userBanService.unban(id, AuthUtil.requireUser(request));
        return ResultDTO.okOf();
    }
}
