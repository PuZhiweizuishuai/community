package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.SysPermission;
import com.buguagaoshu.community.model.SysRole;
import com.buguagaoshu.community.service.RbacService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台角色与权限管理接口。
 *
 * <p>角色定义 / 角色权限维护需要 {@code admin:role}；
 * 给用户分配角色需要 {@code admin:user:role}；
 * 角色下拉选项只暴露非敏感名称，后台任一角色均可读取。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminRoleApiController {

    public record RoleSaveRequest(
            Long id,
            @NotBlank(message = "角色编码不能为空")
            @Size(max = 32, message = "角色编码不能超过 32 个字符") String code,
            @NotBlank(message = "角色名称不能为空")
            @Size(max = 64, message = "角色名称不能超过 64 个字符") String name,
            @Size(max = 255, message = "角色说明不能超过 255 个字符") String description,
            Integer status, Integer sortOrder) {
    }

    public record RolePermissionRequest(
            @NotNull(message = "角色 ID 不能为空")
            @Positive(message = "角色 ID 不合法") Long roleId,
            List<Long> permissionIds) {
    }

    public record UserRoleRequest(
            @NotNull(message = "用户 ID 不能为空")
            @Positive(message = "用户 ID 不合法") Long userId,
            List<Long> roleIds) {
    }

    public record IdRequest(
            @NotNull(message = "角色 ID 不能为空")
            @Positive(message = "角色 ID 不合法") Long id) {
    }

    private final RbacService rbacService;

    public AdminRoleApiController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/roles")
    @RequirePermission(PermissionCodes.ADMIN_ROLE)
    public ResultDTO<List<Map<String, Object>>> roles() {
        return ResultDTO.okOf(rbacService.listRoleViews());
    }

    /** 角色下拉选项：用户角色分配弹窗使用 */
    @GetMapping("/roles/options")
    @RequirePermission(PermissionCodes.ADMIN_ACCESS)
    public ResultDTO<List<Map<String, Object>>> roleOptions() {
        List<Map<String, Object>> options = rbacService.listRoles().stream().map(role -> {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", role.getId());
            option.put("code", role.getCode());
            option.put("name", role.getName());
            option.put("builtIn", role.getBuiltIn() == 1);
            option.put("status", role.getStatus());
            return option;
        }).toList();
        return ResultDTO.okOf(options);
    }

    @GetMapping("/permissions")
    @RequirePermission(PermissionCodes.ADMIN_ROLE)
    public ResultDTO<Map<String, Object>> permissions() {
        List<SysPermission> permissions = rbacService.listPermissions();
        Map<String, List<SysPermission>> grouped = new LinkedHashMap<>();
        for (SysPermission permission : permissions) {
            grouped.computeIfAbsent(permission.getGroupName(), key -> new ArrayList<>()).add(permission);
        }
        List<Map<String, Object>> groups = new ArrayList<>();
        grouped.forEach((groupName, items) -> {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("name", groupName);
            group.put("items", items);
            groups.add(group);
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", permissions);
        data.put("groups", groups);
        return ResultDTO.okOf(data);
    }

    @GetMapping("/roles/{id}/permissions")
    @RequirePermission(PermissionCodes.ADMIN_ROLE)
    public ResultDTO<Map<String, Object>> rolePermissions(@PathVariable("id") long id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("permissionIds", rbacService.listRolePermissionIds(id));
        return ResultDTO.okOf(data);
    }

    @PostMapping("/roles/save")
    @RequirePermission(PermissionCodes.ADMIN_ROLE)
    public ResultDTO<SysRole> saveRole(@Valid @RequestBody RoleSaveRequest body) {
        SysRole role = rbacService.saveRole(new RbacService.RoleSaveCommand(
                body.id(), body.code(), body.name(), body.description(), body.status(), body.sortOrder()));
        return ResultDTO.okOf(role);
    }

    @PostMapping("/roles/delete")
    @RequirePermission(PermissionCodes.ADMIN_ROLE)
    public ResultDTO<Object> deleteRole(@Valid @RequestBody IdRequest body) {
        rbacService.deleteRole(body.id());
        return ResultDTO.okOf();
    }

    @PostMapping("/roles/permissions")
    @RequirePermission(PermissionCodes.ADMIN_ROLE)
    public ResultDTO<Object> saveRolePermissions(@Valid @RequestBody RolePermissionRequest body) {
        rbacService.saveRolePermissions(body.roleId(), body.permissionIds());
        return ResultDTO.okOf();
    }

    @GetMapping("/users/{id}/roles")
    @RequirePermission(PermissionCodes.ADMIN_USER_ROLE)
    public ResultDTO<Map<String, Object>> userRoles(@PathVariable("id") long id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roleIds", rbacService.listUserRoleIds(id));
        return ResultDTO.okOf(data);
    }

    @PostMapping("/users/roles")
    @RequirePermission(PermissionCodes.ADMIN_USER_ROLE)
    public ResultDTO<Object> saveUserRoles(@Valid @RequestBody UserRoleRequest body) {
        rbacService.saveUserRoles(body.userId(), body.roleIds());
        return ResultDTO.okOf();
    }
}
