package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ClickLikeDTO;
import com.buguagaoshu.community.dto.QuestionAccessDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.enums.ClickLikeTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ClickLikeService;
import com.buguagaoshu.community.service.QuestionAccessService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 点赞 API（问题或评论）。身份取自会话，不再需要前端传 token。
 */
@RestController
@Slf4j
public class ClickLikeApiController {

    private final ClickLikeService clickLikeService;
    private final QuestionMapper questionMapper;
    private final QuestionAccessService questionAccessService;

    public ClickLikeApiController(ClickLikeService clickLikeService,
                                  QuestionMapper questionMapper,
                                  QuestionAccessService questionAccessService) {
        this.clickLikeService = clickLikeService;
        this.questionMapper = questionMapper;
        this.questionAccessService = questionAccessService;
    }

    @PostMapping({"/api/like", "/api/clickLike"})
    public ResultDTO<Map<String, Object>> clickLike(@Valid @RequestBody ClickLikeDTO body,
                                                    HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        Question question = questionMapper.selectQuestionById(body.getQuestionId(), 1);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        QuestionAccessDto access = questionAccessService.resolve(question, user, null);
        if (!access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        if (!access.canViewContent()) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        body.setNotifier(user.getId());
        body.setNotifierName(user.getUserName());
        if (body.getCommentId() == null) {
            body.setCommentId(-1L);
        }
        ClickLikeTypeEnum type = clickLikeService.createClickLike(body);
        if (type == ClickLikeTypeEnum.SUCCESS || type == ClickLikeTypeEnum.SUCCESS_CANCEL) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("liked", type == ClickLikeTypeEnum.SUCCESS);
            data.put("message", type.getName());
            return ResultDTO.okOf(data);
        }
        CustomizeErrorCode errorCode = type == ClickLikeTypeEnum.CLICK_LIKE_IS_NOT_USER
                ? CustomizeErrorCode.PARAM_ERROR : CustomizeErrorCode.NOT_FOUND;
        return ResultDTO.errorOf(errorCode.getCode(), type.getName());
    }
}
