package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.AdminDataDTO;
import com.buguagaoshu.community.dto.AdminQuestionDetailDto;
import com.buguagaoshu.community.dto.AdvertisementBatchDTO;
import com.buguagaoshu.community.dto.AdvertisementOperationResult;
import com.buguagaoshu.community.dto.CreateAdvertisement;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.model.Advertisement;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdminDataService;
import com.buguagaoshu.community.service.AdminOverviewService;
import com.buguagaoshu.community.service.AdvertisementService;
import com.buguagaoshu.community.service.LogService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理页面数据接口（对应原 admin/*.html）。
 * 权限由 {@code @RequirePermission} + PermissionInterceptor 统一校验。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminPageApiController {

    /** 广告投放/结束投放请求：id 必填，position/time 仅投放时使用 */
    public record AdvertisementActionRequest(
            @NotNull(message = "广告 ID 不能为空")
            @Positive(message = "广告 ID 不合法") Long id,
            String position,
            Long time) {
    }

    private final AdminDataService adminDataService;
    private final AdminOverviewService adminOverviewService;
    private final QuestionService questionService;
    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final LogService logService;
    private final AdvertisementService advertisementService;

    public AdminPageApiController(AdminDataService adminDataService,
                                  AdminOverviewService adminOverviewService,
                                  QuestionService questionService,
                                  UserService userService,
                                  OnlineUserService onlineUserService,
                                  LogService logService,
                                  AdvertisementService advertisementService) {
        this.adminDataService = adminDataService;
        this.adminOverviewService = adminOverviewService;
        this.questionService = questionService;
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.logService = logService;
        this.advertisementService = advertisementService;
    }

    /** 当前管理员登录态（前端路由守卫使用，不强制要求已登录） */
    @GetMapping("/session")
    public ResultDTO<Map<String, Object>> session(HttpServletRequest request) {
        User admin = AuthUtil.currentAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("admin", AuthUtil.safeUser(admin));
        data.put("isAdmin", admin != null);
        data.put("permissions", AuthUtil.permissionsOf(admin));
        return ResultDTO.okOf(data);
    }

    /** 仪表盘：统计卡 + 近期数据表格（ECharts 数据源） */
    @GetMapping("/dashboard")
    @RequirePermission(PermissionCodes.ADMIN_ACCESS)
    public ResultDTO<Map<String, Object>> dashboard(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userNumber", userService.getAlluserCount());
        data.put("questionNumber", questionService.getAllQuestionCount());
        data.put("onlineUser", onlineUserService.selectOnlineUserCount());
        data.put("overview", adminOverviewService.overview());
        PaginationDto<AdminDataDTO> pagination = adminDataService.selectAdminData(page, size);
        data.put("pagination", pagination);
        return ResultDTO.okOf(data);
    }

    @GetMapping("/users")
    @RequirePermission(PermissionCodes.ADMIN_USER)
    public ResultDTO<PaginationDto<User>> users(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(userService.getUserList(page, size));
    }

    @GetMapping("/questions")
    @RequirePermission(PermissionCodes.ADMIN_QUESTION)
    public ResultDTO<PaginationDto<QuestionDto>> questions(
            @RequestParam(value = "status", defaultValue = "-1") String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(questionService.getAdminQuestionList(status, keyword, page, size));
    }

    /** 问题详情：含全部正文段落，供后台判断内容是否违规后再删除/恢复 */
    @GetMapping("/questions/{id}")
    @RequirePermission(PermissionCodes.ADMIN_QUESTION)
    public ResultDTO<AdminQuestionDetailDto> questionDetail(
            @PathVariable("id") @Positive(message = "问题 ID 不合法") long id,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(questionService.getAdminQuestionDetail(id));
    }

    /** 控制面板：日志列表 */
    @GetMapping("/controller")
    @RequirePermission(PermissionCodes.ADMIN_SYSTEM)
    public ResultDTO<Map<String, Object>> controller(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("logs", logService.getLogDtoList());
        return ResultDTO.okOf(data);
    }

    @GetMapping("/advertisements")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<PaginationDto<Advertisement>> advertisements(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(advertisementService.getAdvertisementList(page, size));
    }

    @GetMapping("/advertisements/{id}")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Advertisement> advertisement(@PathVariable("id") long id,
                                                  HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(advertisementService.selectAdvertisementById(id));
    }

    // ------------------------------------------------------------------
    // 广告维护（原 AdvertisementController 中的 POST 接口）
    // ------------------------------------------------------------------

    @PostMapping("/advertisement/add")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Object> add(@Valid @RequestBody CreateAdvertisement body, HttpServletRequest request) {
        boolean ok = advertisementService.insertAdvertisement(body, AuthUtil.requireAdmin(request)) == 1;
        return ok ? ResultDTO.okOf() : ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
    }

    @PostMapping("/advertisement/alter")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Object> alter(@Valid @RequestBody CreateAdvertisement body, HttpServletRequest request) {
        boolean ok = advertisementService.alterAdvertisement(body, AuthUtil.requireAdmin(request)) == 1;
        return ok ? ResultDTO.okOf() : ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
    }

    /** 各投放位置容量 / 已用 / 剩余，供后台批量投放前查看 */
    @GetMapping("/advertisement/positions")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Map<String, Object>> positions(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(Map.of("positions", advertisementService.positionStats()));
    }

    @PostMapping("/advertisement/setting")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Object> setting(@Valid @RequestBody AdvertisementActionRequest body,
                                     HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        AdvertisementBatchDTO batch = new AdvertisementBatchDTO();
        batch.setIds(List.of(body.id()));
        batch.setPosition(body.position());
        batch.setTime(body.time());
        AdvertisementOperationResult result = advertisementService.batchSetting(batch, admin).get(0);
        return result.isSuccess() ? ResultDTO.okOf()
                : ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), result.getMessage());
    }

    @PostMapping("/advertisement/close")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Object> close(@Valid @RequestBody AdvertisementActionRequest body,
                                   HttpServletRequest request) {
        AdvertisementOperationResult result = advertisementService.batchClose(
                List.of(body.id()), AuthUtil.requireAdmin(request)).get(0);
        return result.isSuccess() ? ResultDTO.okOf()
                : ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), result.getMessage());
    }

    /** 批量投放：返回逐条回执 + 汇总 */
    @PostMapping("/advertisement/batch-setting")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Map<String, Object>> batchSetting(@RequestBody AdvertisementBatchDTO body,
                                                       HttpServletRequest request) {
        List<AdvertisementOperationResult> results =
                advertisementService.batchSetting(body, AuthUtil.requireAdmin(request));
        return ResultDTO.okOf(summarize(results, "批量投放"));
    }

    /** 批量结束投放 */
    @PostMapping("/advertisement/batch-close")
    @RequirePermission(PermissionCodes.ADMIN_ADVERTISEMENT)
    public ResultDTO<Map<String, Object>> batchClose(@RequestBody AdvertisementBatchDTO body,
                                                     HttpServletRequest request) {
        List<AdvertisementOperationResult> results =
                advertisementService.batchClose(body.getIds(), AuthUtil.requireAdmin(request));
        return ResultDTO.okOf(summarize(results, "批量结束"));
    }

    private Map<String, Object> summarize(List<AdvertisementOperationResult> results, String action) {
        long success = results.stream().filter(AdvertisementOperationResult::isSuccess).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", action);
        data.put("total", results.size());
        data.put("successCount", success);
        data.put("failedCount", results.size() - success);
        data.put("results", results);
        return data;
    }
}
