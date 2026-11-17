package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.FileDTO;
import com.buguagaoshu.community.dto.ResultDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
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
    @ResponseBody
    public FileDTO updateUserHead(String imgBase,
                                  HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return new FileDTO(0, "请先登陆", "");
        }

        String name = UUID.randomUUID().toString() + ".jpeg";
        MultipartFile file = base64toMultipart(imgBase, name);
        if (file == null || file.isEmpty()) {
            return new FileDTO(0, "图片内容为空或格式不正确", "");
        }
        try {
            FileRecord record = fileRepository.upload(file, user);
            userService.updateUserHeadUrlById(user.getId(), record.getUrl());
            user.setHeadUrl(record.getUrl());
            request.getSession().setAttribute("user", user);
            return new FileDTO(1, "上传成功", record.getUrl());
        } catch (CustomizeException e) {
            return new FileDTO(0, e.getMessage(), "");
        } catch (Exception e) {
            log.error("头像文件保存失败：{}", e.getMessage());
            return new FileDTO(0, "上传失败，请重试", "");
        }
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
    public ResultDTO<Object> alterUserData(String userName, String sex,
                                           String school, String major,
                                           String simpleSelfIntroduction,
                                           String selfIntroduction, String like,
                                           HttpServletRequest request) {

        User user = AuthUtil.requireUser(request);
        if (sex == null || sex.equals("")) {
            sex = user.getSex();
        }
        if(userName != null && !userName.equals("")) {
            if (userName.length() > 20) {
                return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "昵称不能超过 20 个字符");
            }
            user.setUserName(userName);
        }
        user.setSex(sex);

        if(school != null && !school.equals("")) {
            user.setSchool(school);
        }

        if(major != null && !major.equals("")) {
            user.setMajor(major);
        }


        if(simpleSelfIntroduction != null && simpleSelfIntroduction.length() >= 50) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "个性签名超出限定长度");
        }
        if(selfIntroduction != null && selfIntroduction.length() >= 500) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "简介超出限定长度");
        }
        user.setSimpleSelfIntroduction(simpleSelfIntroduction);
        user.setSelfIntroduction(selfIntroduction);
        user.setLikes(like);
        userMapper.updateUserData(user);
        request.getSession().setAttribute("user", user);
        return ResultDTO.okOf();
    }


    /**
     * 更新密码：成功后吊销该用户全部「记住我」token 与当前会话，需重新登录。
     * */
    @PostMapping("/api/update/password")
    public ResultDTO<Object> updatePassword(String oldPassword, String newPassword, String CAPTCHA,
                                            HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);

        if(!CaptchaUtil.ver(CAPTCHA, request)) {
            return ResultDTO.errorOf(CustomizeErrorCode.CAPTCHA_REQUIRED);
        }
        if (oldPassword == null || newPassword == null || oldPassword.equals(newPassword)) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "两次输入密码相同！");
        }

        if (!StringUtil.checkPassword(newPassword)) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "密码格式错误！");
        }
        user = userService.selectUserById(user.getId());
        if (user == null) {
            return ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND);
        }
        if (!StringUtil.judgePassword(oldPassword, user.getPassword())) {
            return ResultDTO.errorOf(CustomizeErrorCode.SIGN_IN_PASSWORD_ERROR.getCode(), "原密码错误！");
        }
        if (StringUtil.judgePassword(newPassword, user.getPassword())) {
            return ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "新密码与旧密码相同！无需修改");
        }
        if (userService.updateUserPasswordById(user.getId(), newPassword) != 1) {
            return ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR.getCode(), "修改失败，请稍后重试！");
        }
        // 吊销旧 token（在线记录）与所有会话属性，强制重新登录
        onlineUserService.deleteOnlineUserById(user.getId());
        request.getSession().invalidate();
        return ResultDTO.okOf();
    }
}
