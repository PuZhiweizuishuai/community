package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.AdminCommentDetailDto;
import com.buguagaoshu.community.dto.AdminCommentDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdminCommentService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台评论管理：查看全部评论（含已删除）、评论上下文、删除与恢复。
 */
@RestController
@RequestMapping("/api/admin/comments")
@RequirePermission(PermissionCodes.ADMIN_COMMENT)
public class AdminCommentApiController {

    private final AdminCommentService adminCommentService;

    public AdminCommentApiController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @GetMapping
    public ResultDTO<PaginationDto<AdminCommentDto>> list(
            @RequestParam(value = "status", defaultValue = "-1") String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(adminCommentService.listForAdmin(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResultDTO<AdminCommentDetailDto> detail(
            @PathVariable("id") @Positive(message = "评论 ID 不合法") long id,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(adminCommentService.detail(id));
    }

    @PostMapping("/{id}/delete")
    public ResultDTO<Object> delete(
            @PathVariable("id") @Positive(message = "评论 ID 不合法") long id,
            HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        adminCommentService.delete(id, admin);
        return ResultDTO.okOf();
    }

    @PostMapping("/{id}/recover")
    public ResultDTO<Object> recover(
            @PathVariable("id") @Positive(message = "评论 ID 不合法") long id,
            HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        adminCommentService.recover(id, admin);
        return ResultDTO.okOf();
    }
}
