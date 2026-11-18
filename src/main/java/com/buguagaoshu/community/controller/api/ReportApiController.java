package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ContentReportCreateDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ContentReportService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧举报入口：登录用户可举报问题与评论。
 */
@RestController
public class ReportApiController {

    private final ContentReportService contentReportService;

    public ReportApiController(ContentReportService contentReportService) {
        this.contentReportService = contentReportService;
    }

    @PostMapping("/api/reports")
    @RequirePermission(PermissionCodes.CONTENT_REPORT_CREATE)
    public ResultDTO<Object> create(@Valid @RequestBody ContentReportCreateDto body,
                                    HttpServletRequest request) {
        User reporter = AuthUtil.requireUser(request);
        contentReportService.create(body, reporter);
        return ResultDTO.okOf();
    }
}
