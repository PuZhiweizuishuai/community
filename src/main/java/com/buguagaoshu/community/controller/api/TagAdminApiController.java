package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.TagAdminDto;
import com.buguagaoshu.community.model.Tag;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.TagAdminService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台标签管理：列表 / 审核 / 重命名 / 删除。
 */
@RestController
@RequestMapping("/api/admin/tags")
@RequirePermission(PermissionCodes.ADMIN_TAG)
public class TagAdminApiController {

    public record IdRequest(
            @NotNull(message = "标签 ID 不能为空")
            @Positive(message = "标签 ID 不合法") Long id) {
    }

    public record RejectRequest(
            @NotNull(message = "标签 ID 不能为空")
            @Positive(message = "标签 ID 不合法") Long id,
            @NotBlank(message = "请填写不通过原因")
            @Size(max = 200, message = "不通过原因不能超过 200 个字符") String reason) {
    }

    public record RenameRequest(
            @NotNull(message = "标签 ID 不能为空")
            @Positive(message = "标签 ID 不合法") Long id,
            @NotBlank(message = "标签名称不能为空")
            @Size(max = 50, message = "标签名称不能超过 50 个字符") String name) {
    }

    private final TagAdminService tagAdminService;

    public TagAdminApiController(TagAdminService tagAdminService) {
        this.tagAdminService = tagAdminService;
    }

    @GetMapping
    public ResultDTO<PaginationDto<TagAdminDto>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(tagAdminService.list(status, keyword, page, size));
    }

    @PostMapping("/approve")
    public ResultDTO<Tag> approve(@Valid @RequestBody IdRequest body, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(tagAdminService.approve(body.id(), admin));
    }

    @PostMapping("/reject")
    public ResultDTO<Tag> reject(@Valid @RequestBody RejectRequest body, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(tagAdminService.reject(body.id(), body.reason(), admin));
    }

    @PostMapping("/rename")
    public ResultDTO<Tag> rename(@Valid @RequestBody RenameRequest body, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(tagAdminService.rename(body.id(), body.name(), admin));
    }

    @PostMapping("/delete")
    public ResultDTO<Map<String, Object>> delete(@Valid @RequestBody IdRequest body,
                                                 HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        tagAdminService.delete(body.id(), admin);
        return ResultDTO.okOf(Map.of("deleted", true));
    }
}
