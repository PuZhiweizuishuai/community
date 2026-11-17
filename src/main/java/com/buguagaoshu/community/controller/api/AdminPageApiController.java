package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.AdminDataDTO;
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
import com.buguagaoshu.community.service.AdvertisementService;
import com.buguagaoshu.community.service.LogService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
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
 * 所有接口都要求会话中存在 admin，未登录返回 401 + code 401。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminPageApiController {

    private final AdminDataService adminDataService;
    private final QuestionService questionService;
    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final LogService logService;
    private final AdvertisementService advertisementService;

    public AdminPageApiController(AdminDataService adminDataService,
                                  QuestionService questionService,
                                  UserService userService,
                                  OnlineUserService onlineUserService,
                                  LogService logService,
                                  AdvertisementService advertisementService) {
        this.adminDataService = adminDataService;
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
        return ResultDTO.okOf(data);
    }

    /** 仪表盘：统计卡 + 近期数据表格（ECharts 数据源） */
    @GetMapping("/dashboard")
    public ResultDTO<Map<String, Object>> dashboard(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userNumber", userService.getAlluserCount());
        data.put("questionNumber", questionService.getAllQuestionCount());
        data.put("onlineUser", onlineUserService.selectOnlineUserCount());
        PaginationDto<AdminDataDTO> pagination = adminDataService.selectAdminData(page, size);
        data.put("pagination", pagination);
        return ResultDTO.okOf(data);
    }

    @GetMapping("/users")
    public ResultDTO<PaginationDto<User>> users(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(userService.getUserList(page, size));
    }

    @GetMapping("/questions")
    public ResultDTO<PaginationDto<QuestionDto>> questions(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(questionService.getAllQuestionList(page, size));
    }

    /** 后台搜索，按 type 返回问题或用户分页 */
    @GetMapping("/search")
    public ResultDTO<Map<String, Object>> search(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            @RequestParam(value = "type", defaultValue = "question") String type,
            @RequestParam(value = "search", required = false) String keyword,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("search", keyword);
        data.put("type", type);
        if (StringUtils.isBlank(keyword)) {
            data.put("questions", null);
            data.put("users", null);
            return ResultDTO.okOf(data);
        }
        // LIKE 搜索：关键字清洗在各 Service 内完成，这里不再拼接正则
        data.put("questions", "question".equals(type)
                ? questionService.searchAllQuestionList(keyword, page, size) : null);
        data.put("users", "user".equals(type)
                ? userService.searchUser(keyword, page, size) : null);
        return ResultDTO.okOf(data);
    }

    /** 控制面板：日志列表 */
    @GetMapping("/controller")
    public ResultDTO<Map<String, Object>> controller(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("logs", logService.getLogDtoList());
        return ResultDTO.okOf(data);
    }

    @GetMapping("/advertisements")
    public ResultDTO<PaginationDto<Advertisement>> advertisements(
            @RequestParam(value = "page", defaultValue = "1") String page,
            @RequestParam(value = "size", defaultValue = "20") String size,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(advertisementService.getAdvertisementList(page, size));
    }

    @GetMapping("/advertisements/{id}")
    public ResultDTO<Advertisement> advertisement(@PathVariable("id") long id,
                                                  HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(advertisementService.selectAdvertisementById(id));
    }

    // ------------------------------------------------------------------
    // 广告维护（原 AdvertisementController 中的 POST 接口）
    // ------------------------------------------------------------------

    @PostMapping("/advertisement/add")
    public ResultDTO<Object> add(@RequestBody CreateAdvertisement body, HttpServletRequest request) {
        boolean ok = advertisementService.insertAdvertisement(body, AuthUtil.requireAdmin(request)) == 1;
        return ok ? ResultDTO.okOf() : ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
    }

    @PostMapping("/advertisement/alter")
    public ResultDTO<Object> alter(@RequestBody CreateAdvertisement body, HttpServletRequest request) {
        boolean ok = advertisementService.alterAdvertisement(body, AuthUtil.requireAdmin(request)) == 1;
        return ok ? ResultDTO.okOf() : ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
    }

    /** 各投放位置容量 / 已用 / 剩余，供后台批量投放前查看 */
    @GetMapping("/advertisement/positions")
    public ResultDTO<Map<String, Object>> positions(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return ResultDTO.okOf(Map.of("positions", advertisementService.positionStats()));
    }

    @PostMapping("/advertisement/setting")
    public ResultDTO<Object> setting(@RequestBody CreateAdvertisement body, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        if (body.getId() == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
        AdvertisementBatchDTO batch = new AdvertisementBatchDTO();
        batch.setIds(List.of(body.getId()));
        batch.setPosition(body.getPosition());
        batch.setTime(body.getTime());
        AdvertisementOperationResult result = advertisementService.batchSetting(batch, admin).get(0);
        return result.isSuccess() ? ResultDTO.okOf()
                : ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), result.getMessage());
    }

    @PostMapping("/advertisement/close")
    public ResultDTO<Object> close(@RequestBody CreateAdvertisement body, HttpServletRequest request) {
        if (body.getId() == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
        AdvertisementOperationResult result = advertisementService.batchClose(
                List.of(body.getId()), AuthUtil.requireAdmin(request)).get(0);
        return result.isSuccess() ? ResultDTO.okOf()
                : ResultDTO.errorOf(CustomizeErrorCode.CONFLICT.getCode(), result.getMessage());
    }

    /** 批量投放：返回逐条回执 + 汇总 */
    @PostMapping("/advertisement/batch-setting")
    public ResultDTO<Map<String, Object>> batchSetting(@RequestBody AdvertisementBatchDTO body,
                                                       HttpServletRequest request) {
        List<AdvertisementOperationResult> results =
                advertisementService.batchSetting(body, AuthUtil.requireAdmin(request));
        return ResultDTO.okOf(summarize(results, "批量投放"));
    }

    /** 批量结束投放 */
    @PostMapping("/advertisement/batch-close")
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
