package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.FollowQuestionDTO;
import com.buguagaoshu.community.dto.FollowTopicDTO;
import com.buguagaoshu.community.dto.FollowUserDTO;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.FollowQuestionService;
import com.buguagaoshu.community.service.FollowTopicService;
import com.buguagaoshu.community.service.FollowUserService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 关注 API（话题 / 问题 / 用户）。
 *
 * <p>前后端分离后身份统一取自 Spring Session，不再要求前端把 JWT token 放进请求体；
 * 保留旧路径作为别名兼容。
 */
@RestController
public class FollowApiController {

    private final TagClassCache tagClassCache;
    private final FollowTopicService followTopicService;
    private final QuestionMapper questionMapper;
    private final FollowQuestionService followQuestionService;
    private final FollowUserService followUserService;
    private final UserService userService;

    public FollowApiController(TagClassCache tagClassCache,
                               FollowTopicService followTopicService,
                               QuestionMapper questionMapper,
                               FollowQuestionService followQuestionService,
                               FollowUserService followUserService,
                               UserService userService) {
        this.tagClassCache = tagClassCache;
        this.followTopicService = followTopicService;
        this.questionMapper = questionMapper;
        this.followQuestionService = followQuestionService;
        this.followUserService = followUserService;
        this.userService = userService;
    }

    @PostMapping({"/api/follow/topic", "/api/followTopic"})
    public ResultDTO<Map<String, Object>> followTopic(@Valid @RequestBody FollowTopicDTO body,
                                                      HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        Map<String, TagClass> tagMap = tagClassCache.getTagClassMap();
        TagClass tagClass = tagMap == null ? null : tagMap.get(body.getTopicTitle());
        if (tagClass == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND.getCode(), "话题不存在了，请选择别的话题吧!");
        }
        int result = followTopicService.insertFollowTop(body, user.getId(), tagClass);
        return result(result);
    }

    @PostMapping({"/api/follow/question", "/api/followQuestion"})
    public ResultDTO<Map<String, Object>> followQuestion(@Valid @RequestBody FollowQuestionDTO body,
                                                         HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        Question question = questionMapper.selectQuestionById(body.getFollowQuestionId(), 1);
        if (question == null || question.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND.getCode(), "问题不存在了，请选择别的问题吧!");
        }
        int result = followQuestionService.insertFollowQuestion(body, question, user.getId());
        return result(result);
    }

    @PostMapping({"/api/follow/user", "/api/followUser"})
    public ResultDTO<Map<String, Object>> followUser(@Valid @RequestBody FollowUserDTO body,
                                                     HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        User target = userService.selectUserById(body.getFollowUserId());
        if (target == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND.getCode(), "你关注的用户不存在！");
        }
        if (target.getId() == user.getId()) {
            return ResultDTO.errorOf(CustomizeErrorCode.INVALID_OPERATION.getCode(), "你不能自己关注自己！");
        }
        int result = followUserService.insertFollowUser(body.getFollowUserId(), user.getId(), target);
        return result(result);
    }

    private ResultDTO<Map<String, Object>> result(int result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("following", result == 1);
        return ResultDTO.okOf(data);
    }
}
