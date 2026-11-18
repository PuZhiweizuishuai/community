package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ContentReportDetailDto;
import com.buguagaoshu.community.dto.ContentReportDto;
import com.buguagaoshu.community.dto.ContentReportHandleDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ContentReportService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台举报管理：列表、详情、受理与驳回。
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequirePermission(PermissionCodes.ADMIN_REPORT)
public class AdminReportApiController {

    private final ContentReportService contentReportService;

    public AdminReportApiController(ContentReportService contentReportService) {
        this.contentReportService = contentReportService;
    }

    @GetMapping
    public ResultDTO<PaginationDto<ContentReportDto>> list(
            @RequestParam(value = "status", defaultValue = "0") String status,
            @RequestParam(value = "type", defaultValue = "all") String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(contentReportService.listForAdmin(status, type, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResultDTO<ContentReportDetailDto> detail(
            @PathVariable("id") @Positive(message = "举报 ID 不合法") long id,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(contentReportService.detail(id));
    }

    @PostMapping("/{id}/handle")
    public ResultDTO<Object> handle(
            @PathVariable("id") @Positive(message = "举报 ID 不合法") long id,
            @Valid @RequestBody ContentReportHandleDto body,
            HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        contentReportService.handle(id, body, admin);
        return ResultDTO.okOf();
    }
}
