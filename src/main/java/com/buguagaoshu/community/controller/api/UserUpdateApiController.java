package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.dto.UpdatePasswordRequest;
import com.buguagaoshu.community.dto.UpdateProfileRequest;
import com.buguagaoshu.community.dto.UserPrivacySettingsDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.FileRecord;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.repository.FileRepository;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.AuthUtil;
import com.buguagaoshu.community.util.Base64MultipartFile;
import com.buguagaoshu.community.util.StringUtil;
import com.buguagaoshu.community.captcha.CaptchaUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.UUID;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 22:35
 */
@RestController
@Slf4j
public class UserUpdateApiController {

    final
    UserService userService;

    final
    UserMapper userMapper;

    private final FileRepository fileRepository;

    private final OnlineUserService onlineUserService;

    @Autowired
    public UserUpdateApiController(UserService userService, FileRepository fileRepository, UserMapper userMapper, OnlineUserService onlineUserService) {
        this.userService = userService;
        this.fileRepository = fileRepository;
        this.userMapper = userMapper;
        this.onlineUserService = onlineUserService;
    }

    /**
     * 更新头像：base64 转文件后走统一文件上传编排
     */
    @PostMapping("/api/update/HeadUrl")
    public ResultDTO<String> updateUserHead(String imgBase, HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);

        String name = UUID.randomUUID().toString() + ".jpeg";
        MultipartFile file = base64toMultipart(imgBase, name);
        if (file == null || file.isEmpty()) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "图片内容为空或格式不正确");
        }
        FileRecord record = fileRepository.upload(file, user);
        userService.updateUserHeadUrlById(user.getId(), record.getUrl());
        user.setHeadUrl(record.getUrl());
        request.getSession().setAttribute("user", user);
        return ResultDTO.okOf(record.getUrl());
    }


    /**
     * Base64转化为MultipartFile
     *
     * @param data     前台传过来base64的编码
     * @param fileName 图片的文件名
     * @return 图片文件
     */
    public MultipartFile base64toMultipart(String data, String fileName) {
        try {
            String[] baseStrs = data.split(",");
            if (baseStrs.length < 2) {
                return null;
            }
            byte[] b = Base64.getDecoder().decode(baseStrs[1]);
            return new Base64MultipartFile(b, baseStrs[0], fileName);
        } catch (Exception e) {
            log.error("图片文件转换:  {}",e.getMessage());
            return null;
        }
    }

    /**
     * 更新用户信息
     * */
    @PostMapping("/api/update/data")
    public ResultDTO<Object> alterUserData(@Valid UpdateProfileRequest body, HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        if (StringUtils.isNotBlank(body.getUserName())) {
            user.setUserName(body.getUserName());
        }
        if (StringUtils.isNotBlank(body.getSex())) {
            user.setSex(body.getSex());
        }
        if (StringUtils.isNotBlank(body.getSchool())) {
            user.setSchool(body.getSchool());
        }
        if (StringUtils.isNotBlank(body.getMajor())) {
            user.setMajor(body.getMajor());
        }
        // 简介类字段允许通过空串清空，未传（null）时保持原值
        if (body.getSimpleSelfIntroduction() != null) {
            user.setSimpleSelfIntroduction(body.getSimpleSelfIntroduction());
        }
        if (body.getSelfIntroduction() != null) {
            user.setSelfIntroduction(body.getSelfIntroduction());
        }
        if (body.getLike() != null) {
            user.setLikes(body.getLike());
        }
        userMapper.updateUserData(user);
        request.getSession().setAttribute("user", user);
        return ResultDTO.okOf();
    }


    /**
     * 更新个人主页隐私开关。
     */
    @PostMapping("/api/update/privacy")
    public ResultDTO<Object> updatePrivacy(@Valid @RequestBody UserPrivacySettingsDTO body,
                                           HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        int showFollowQuestions = Boolean.TRUE.equals(body.getShowFollowQuestions()) ? 1 : 0;
        int showFollowTopics = Boolean.TRUE.equals(body.getShowFollowTopics()) ? 1 : 0;
        int showFans = Boolean.TRUE.equals(body.getShowFans()) ? 1 : 0;
        int showFollowingUsers = Boolean.TRUE.equals(body.getShowFollowingUsers()) ? 1 : 0;
        userMapper.updatePrivacySettings(user.getId(), showFollowQuestions, showFollowTopics,
                showFans, showFollowingUsers);
        user.setShowFollowQuestions(showFollowQuestions);
        user.setShowFollowTopics(showFollowTopics);
        user.setShowFans(showFans);
        user.setShowFollowingUsers(showFollowingUsers);
        request.getSession().setAttribute("user", user);
        return ResultDTO.okOf();
    }

    /**
     * 更新密码：成功后吊销该用户全部「记住我」token 与当前会话，需重新登录。
     * */
    @PostMapping("/api/update/password")
    public ResultDTO<Object> updatePassword(@Valid UpdatePasswordRequest body, HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);

        if (!CaptchaUtil.ver(body.getCAPTCHA(), request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }
        if (body.getOldPassword().equals(body.getNewPassword())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "两次输入密码相同！");
        }

        user = userService.selectUserById(user.getId());
        if (user == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        if (!StringUtil.judgePassword(body.getOldPassword(), user.getPassword())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "原密码错误！");
        }
        if (StringUtil.judgePassword(body.getNewPassword(), user.getPassword())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "新密码与旧密码相同！无需修改");
        }
        if (userService.updateUserPasswordById(user.getId(), body.getNewPassword()) != 1) {
            return ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR.getCode(), "修改失败，请稍后重试！");
        }
        // 吊销旧 token（在线记录）与所有会话属性，强制重新登录
        onlineUserService.deleteOnlineUserById(user.getId());
        request.getSession().invalidate();
        return ResultDTO.okOf();
    }
}
