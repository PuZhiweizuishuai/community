package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.IndexTopQuestion;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
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


    @PostMapping("/api/admin/delete/question")
    public ResultDTO<Object> deleteQuestion(Long questionId, HttpServletRequest request) {
        if (questionId == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
        User admin = AuthUtil.currentAdmin(request);
        User user = AuthUtil.currentUser(request);
        if (admin == null && user == null) {
            throw new CustomizeException(CustomizeErrorCode.NO_LOGIN);
        }
        User operator = admin != null ? admin : user;
        try {
            questionService.deleteQuestion(questionId, operator, admin != null);
            return ResultDTO.okOf();
        } catch (CustomizeException e) {
            return ResultDTO.errorOf(e);
        }
    }


    @PostMapping("/api/admin/recovery/question")
    public ResultDTO<Object> recoveryQuestion(Long questionId, HttpServletRequest request) {
        if (questionId == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
        User admin = AuthUtil.requireAdmin(request);
        try {
            questionService.recoveryQuestion(questionId, admin);
            return ResultDTO.okOf();
        } catch (CustomizeException e) {
            return ResultDTO.errorOf(e);
        }
    }


    @PostMapping("/api/admin/update/user")
    public ResultDTO<Object> alterUserPower(Long userId, Integer power, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        // power 取值：0 管理员 / 1 普通用户 / 2 VIP
        if (userId == null || power == null || power < 0 || power > 2) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
        User user = userService.selectUserById(userId);
        if (user == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        userPermissionService.updateUserPermissionById(user.getId(), power, admin.getUserId(), StringUtil.getNowTime());
        return ResultDTO.okOf();
    }

    @PostMapping("/api/admin/reset/password")
    public ResultDTO<Map<String, Object>> resetUserPassword(Long userId, HttpServletRequest request) {
        User admin = AuthUtil.requireAdmin(request);
        if (userId == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
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
    public ResultDTO<Object> settingTopQuestion(Long id1, Long id2, Long id3, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        List<Long> ids = new ArrayList<>();
        for (Long id : new Long[]{id1, id2, id3}) {
            if (id == null) {
                continue;
            }
            if (id < 0) {
                return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
            }
            ids.add(id);
        }
        if (ids.isEmpty()) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR);
        }
        indexTopQuestion.setTopQuestion(ids.toArray(new Long[0]));
        return ResultDTO.okOf();
    }
}