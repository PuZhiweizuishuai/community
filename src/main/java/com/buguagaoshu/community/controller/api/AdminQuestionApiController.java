package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.cache.IndexTopQuestion;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 问题删除/恢复、用户权限与密码管理。业务逻辑在 QuestionService（事务），这里只做鉴权与参数校验。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-06 14:58
 */
@RestController
@Slf4j
public class AdminQuestionApiController {

    private final QuestionService questionService;

    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final OnlineUserService onlineUserService;

    private final IndexTopQuestion indexTopQuestion;

    @Autowired
    public AdminQuestionApiController(QuestionService questionService, UserService userService,
                                      UserPermissionService userPermissionService,
                                      OnlineUserService onlineUserService, IndexTopQuestion indexTopQuestion) {
        this.questionService = questionService;
        this.userService = userService;
        this.userPermissionService = userPermissionService;
        this.onlineUserService = onlineUserService;
        this.indexTopQuestion = indexTopQuestion;
    }


    /**
     * 后台删帖：仅管理员可用。作者删除自己的帖子走 {@code DELETE /api/questions/{questionId}}，
     * 不再共用此路径，避免「普通用户可访问后台接口」的语义混乱。
     */
    @PostMapping("/api/admin/delete/question")
    @RequirePermission(PermissionCodes.ADMIN_QUESTION)
    public ResultDTO<Object> deleteQuestion(@RequestParam("questionId") Long questionId,
                                            HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        try {
            questionService.deleteQuestion(questionId, admin, true);
            return ResultDTO.okOf();
        } catch (CustomizeException e) {
            return ResultDTO.errorOf(e);
        }
    }


    @PostMapping("/api/admin/recovery/question")
    @RequirePermission(PermissionCodes.ADMIN_QUESTION)
    public ResultDTO<Object> recoveryQuestion(@RequestParam("questionId") Long questionId,
                                              HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        try {
            questionService.recoveryQuestion(questionId, admin);
            return ResultDTO.okOf();
        } catch (CustomizeException e) {
            return ResultDTO.errorOf(e);
        }
    }


    /** power 仅保留用户等级语义：1 普通用户 / 2 VIP；管理员身份改为 RBAC 角色分配 */
    @PostMapping("/api/admin/update/user")
    @RequirePermission(PermissionCodes.ADMIN_USER)
    public ResultDTO<Object> alterUserPower(
            @RequestParam("userId") Long userId,
            @RequestParam("power")
            @Min(value = 1, message = "用户等级仅支持普通用户（1）或 VIP（2），管理员请通过角色分配")
            @Max(value = 2, message = "用户等级仅支持普通用户（1）或 VIP（2），管理员请通过角色分配")
            Integer power,
            HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        User user = userService.selectUserById(userId);
        if (user == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        userPermissionService.updateUserPermissionById(user.getId(), power, admin.getUserId(), System.currentTimeMillis());
        return ResultDTO.okOf();
    }

    @PostMapping("/api/admin/reset/password")
    @RequirePermission(PermissionCodes.ADMIN_USER)
    public ResultDTO<Map<String, Object>> resetUserPassword(@RequestParam("userId") Long userId,
                                                            HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        User user = userService.selectUserById(userId);
        if (user == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        // 随机临时密码，不再统一重置为弱口令；同时吊销该用户的「记住我」token
        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        userService.updateUserPasswordById(user.getId(), tempPassword);
        onlineUserService.deleteOnlineUserById(user.getId());
        log.info("管理员 {} 成功重置用户 {} 密码", admin.getId(), user.getId());
        return ResultDTO.okOf(Map.of("tempPassword", tempPassword));
    }


    @PostMapping("/api/admin/settingTopQuestion")
    @RequirePermission(PermissionCodes.ADMIN_QUESTION)
    public ResultDTO<Object> settingTopQuestion(
            @RequestParam(value = "id1", required = false) @Positive(message = "置顶问题 ID 不合法") Long id1,
            @RequestParam(value = "id2", required = false) @Positive(message = "置顶问题 ID 不合法") Long id2,
            @RequestParam(value = "id3", required = false) @Positive(message = "置顶问题 ID 不合法") Long id3,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        List<Long> ids = new ArrayList<>();
        for (Long id : new Long[]{id1, id2, id3}) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
        indexTopQuestion.setTopQuestion(ids.toArray(new Long[0]));
        return ResultDTO.okOf();
    }
}