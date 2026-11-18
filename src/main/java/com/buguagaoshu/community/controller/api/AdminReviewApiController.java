package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.ReviewDetailDto;
import com.buguagaoshu.community.dto.ReviewItemDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ReviewService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台内容审核接口：列表、详情、通过、驳回、待审数量。
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequirePermission(PermissionCodes.ADMIN_REVIEW)
public class AdminReviewApiController {

    public record ActionRequest(
            @NotBlank(message = "审核目标类型不能为空") String type,
            @NotNull(message = "审核目标 ID 不能为空") @Positive(message = "审核目标 ID 不合法") Long id) {
    }

    public record RejectRequest(
            @NotBlank(message = "审核目标类型不能为空") String type,
            @NotNull(message = "审核目标 ID 不能为空") @Positive(message = "审核目标 ID 不合法") Long id,
            @NotBlank(message = "请填写审核不通过原因") @Size(max = 500, message = "审核原因不能超过 500 个字符") String reason,
            @Size(max = 500, message = "修改意见不能超过 500 个字符") String suggestion) {
    }

    private final ReviewService reviewService;

    public AdminReviewApiController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResultDTO<PaginationDto<ReviewItemDto>> list(
            @RequestParam(value = "type", defaultValue = "question") String type,
            @RequestParam(value = "status", defaultValue = "0") String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(reviewService.listForAdmin(type, status, keyword, page, size));
    }

    @GetMapping("/count")
    public ResultDTO<Map<String, Long>> count(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(reviewService.pendingCounts());
    }

    @GetMapping("/{type}/{id}")
    public ResultDTO<ReviewDetailDto> detail(@PathVariable("type") String type,
                                             @PathVariable("id") long id,
                                             HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(reviewService.detail(type, id));
    }

    @PostMapping("/approve")
    public ResultDTO<Object> approve(@Valid @RequestBody ActionRequest body, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        reviewService.approve(body.type(), body.id(), admin);
        return ResultDTO.okOf();
    }

    @PostMapping("/reject")
    public ResultDTO<Object> reject(@Valid @RequestBody RejectRequest body, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        reviewService.reject(body.type(), body.id(), body.reason(), body.suggestion(), admin);
        return ResultDTO.okOf();
    }
}
