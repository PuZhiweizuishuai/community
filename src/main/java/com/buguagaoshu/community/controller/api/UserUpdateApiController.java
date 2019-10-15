package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.FileDTO;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.Base64MultipartFile;
import com.buguagaoshu.community.util.StringUtil;
import com.wf.captcha.utils.CaptchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 22:35
 */
@RestController
@Slf4j
public class UserUpdateApiController {
    @Value("${File.ROOT.PATH}")
    private String ROOT;

    final
    UserService userService;

    @Autowired
    UserMapper userMapper;

    private final ResourceLoader resourceLoader;

    @Autowired
    public UserUpdateApiController(UserService userService, ResourceLoader resourceLoader) {
        this.userService = userService;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 更新头像
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
        String path = ROOT + "/" + user.getUserId() + "/image/head/";
        String pathName ="/" + path + name;
        File dest = new File(path);
        //判断文件父目录是否存在
        if (!dest.exists()) {
            dest.mkdirs();
        }
        try {
            // 保存图片
            Files.copy(file.getInputStream(), Paths.get(path, name));
            userService.updateUserHeadUrlById(user.getId(), pathName);
            user.setHeadUrl(pathName);
            request.getSession().setAttribute("user", user);

            return new FileDTO(1, "上传成功", pathName);
        } catch (Exception e) {
            log.error("头像文件保存失败：{}", e.getMessage());
            return new FileDTO(0, "上传失败，请重试", "");
        }

    }


    /**
     * 显示头像
     */
    @RequestMapping(method = RequestMethod.GET, value = "/file/{userId}/image/head/{filename:.+}", produces = "image/png")
    @ResponseBody
    public ResponseEntity<?> getFile(@PathVariable("filename") String filename,
                                     @PathVariable("userId") String userId) {
        try {
            String path = ROOT + "/" + userId + "/image/head/";
            return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(path, filename)));
        } catch (Exception e) {
            log.error("图片文件获取失败:  {}",e.getMessage());
            return ResponseEntity.notFound().build();
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
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] b = decoder.decode(baseStrs[1]);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
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
    @ResponseBody
    public HashMap<String, Object> alterUserData(String userName, String sex,
                                                 String school, String major,
                                                 String simpleSelfIntroduction,
                                                 String selfIntroduction, String like,
                                                 HttpServletRequest request) {

        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        if (sex == null || sex.equals("")) {
            sex = user.getSex();
        }

        if (sex == null || sex.equals("")) {
            sex = user.getSex();
        }
        if(userName != null && !userName.equals("")) {
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
            return StringUtil.dealResultMessage(false, "个性签名超出限定长度");
        }
        if(selfIntroduction != null && selfIntroduction.length() >= 500) {
            return StringUtil.dealResultMessage(false, "简介超出限定长度");
        }
        user.setSimpleSelfIntroduction(simpleSelfIntroduction);
        user.setSelfIntroduction(selfIntroduction);
        user.setLikes(like);
        try {
            userMapper.updateUserData(user);
            request.getSession().setAttribute("user", user);
            return StringUtil.dealResultMessage(true, "修改成功");
        } catch (Exception e) {
            return StringUtil.dealResultMessage(false, e.getMessage());
        }
    }


    /**
     * 更新密码
     * */
    @PostMapping("/api/update/password")
    public HashMap<String, Object> updatePassword(String oldPassword, String newPassword, String CAPTCHA,
                                                  HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }

        if(!CaptchaUtil.ver(CAPTCHA, request)) {
            CaptchaUtil.clear(request);
            return StringUtil.dealResultMessage(false, "验证码错误！请刷新后重试！");
        }

        if (oldPassword.equals(newPassword)) {
            return StringUtil.dealResultMessage(false, "两次输入密码相同！");
        }


        if (StringUtil.checkPassword(newPassword)) {
           user = userService.selectUserById(user.getId());
            if (user != null) {
                if (StringUtil.judgePassword(oldPassword, user.getPassword())) {
                    if(StringUtil.judgePassword(newPassword, user.getPassword())) {
                        return StringUtil.dealResultMessage(false, "新密码与旧密码相同！无需修改");
                    }

                    if (userService.updateUserPasswordById(user.getId(), newPassword) == 1) {
                        return StringUtil.dealResultMessage(true, "修改成功！");
                    } else {
                        return StringUtil.dealResultMessage(false, "修改失败，请稍后重试！");
                    }
                } else {
                    return StringUtil.dealResultMessage(false, "原密码错误！");
                }
            } else {
                return StringUtil.dealResultMessage(false, "用户不存在！");
            }
        } else {
            return StringUtil.dealResultMessage(false, "密码格式错误！");
        }
    }
}
