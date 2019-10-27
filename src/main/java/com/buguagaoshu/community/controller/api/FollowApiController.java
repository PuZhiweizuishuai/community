package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.FollowQuestionDTO;
import com.buguagaoshu.community.dto.FollowTopicDTO;
import com.buguagaoshu.community.dto.FollowUserDTO;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.FollowQuestionService;
import com.buguagaoshu.community.service.FollowTopicService;
import com.buguagaoshu.community.service.FollowUserService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.JwtUtil;
import com.buguagaoshu.community.util.StringUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-27 12:16
 * 关注 API
 */
@RestController
public class FollowApiController {
    private final JwtUtil jwtUtil;

    private final TagClassCache tagClassCache;

    private final FollowTopicService followTopicService;

    private final QuestionMapper questionMapper;

    private final FollowQuestionService followQuestionService;

    private final FollowUserService followUserService;

    private final UserService userService;

    @Autowired
    public FollowApiController(JwtUtil jwtUtil, TagClassCache tagClassCache, FollowTopicService followTopicService, QuestionMapper questionMapper, FollowQuestionService followQuestionService, FollowUserService followUserService, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.tagClassCache = tagClassCache;
        this.followTopicService = followTopicService;
        this.questionMapper = questionMapper;
        this.followQuestionService = followQuestionService;
        this.followUserService = followUserService;
        this.userService = userService;
    }

    /**
     * 关注话题
     */
    @PostMapping("/api/followTopic")
    public Map<String, Object> followTopic(@RequestBody FollowTopicDTO followTopicDTO) {
        if (followTopicDTO.getToken() == null) {
            return StringUtil.dealResultMessage(false, "请先登陆后再关注");
        }
        try {
            Claims claims = jwtUtil.parseJWT(followTopicDTO.getToken());
            TagClass tagClass = tagClassCache.getTagClassMap().get(followTopicDTO.getTopicTitle());
            if (tagClass == null) {
                return StringUtil.dealResultMessage(false, "话题不存在了，请选择别的话题吧!");
            }
            int result = followTopicService.insertFollowTop(followTopicDTO, claims, tagClass);
            if (result == 1) {
                return StringUtil.dealResultMessage(true, "关注成功！");
            } else {
                return StringUtil.dealResultMessage(true, "取消关注成功！");
            }
        } catch (Exception e) {
            return StringUtil.dealResultMessage(false, "请先登陆后再关注");
        }
    }

    /**
     * 关注问题
     */
    @PostMapping("/api/followQuestion")
    public Map<String, Object> followQuestion(@RequestBody FollowQuestionDTO followQuestionDTO) {
        if (followQuestionDTO.getToken() == null) {
            return StringUtil.dealResultMessage(false, "请先登陆后再关注");
        }
        try {
            Claims claims = jwtUtil.parseJWT(followQuestionDTO.getToken());
            Question question = questionMapper.selectQuestionById(followQuestionDTO.getFollowQuestionId(), 1);
            if (question == null) {
                return StringUtil.dealResultMessage(false, "问题不存在了，请选择别的问题吧!");
            }
            int result = followQuestionService.insertFollowQuestion(followQuestionDTO, question, claims);
            if (result == 1) {
                return StringUtil.dealResultMessage(true, "关注成功！");
            } else {
                return StringUtil.dealResultMessage(true, "取消关注成功！");
            }
        } catch (Exception e) {
            return StringUtil.dealResultMessage(false, "请先登陆后再关注");
        }
    }

    @PostMapping("/api/followUser")
    public Map<String, Object> followUser(@RequestBody FollowUserDTO followUserDTO) {
        if (followUserDTO.getToken() == null) {
            return StringUtil.dealResultMessage(false, "请先登陆后再关注");
        }
        try {
            Claims claims = jwtUtil.parseJWT(followUserDTO.getToken());
            User user = userService.selectUserById(followUserDTO.getFollowUserId());
            if (user == null) {
                return StringUtil.dealResultMessage(false, "你关注的用户不存在！");
            }
            if (user.getId() == Long.parseLong(claims.getId())) {
                return StringUtil.dealResultMessage(false, "你不能自己关注自己！");
            }
            int result = followUserService.insertFollowUser(followUserDTO.getFollowUserId(), claims, user);
            if (result == 1) {
                return StringUtil.dealResultMessage(true, "关注成功！");
            } else {
                return StringUtil.dealResultMessage(true, "取消关注成功！");
            }
        } catch (Exception e) {
            return StringUtil.dealResultMessage(false, "请先登陆后再关注");
        }
    }
}
