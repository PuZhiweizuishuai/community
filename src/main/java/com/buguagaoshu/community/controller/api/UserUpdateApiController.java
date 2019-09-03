package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.FileDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.Base64MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 22:35
 */
@RestController
public class UserUpdateApiController {
    @Value("${File.ROOT.PATH}")
    private String ROOT;

    final
    UserService userService;

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
        //System.out.println(imgBase);
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return new FileDTO(0, "请先登陆", "");
        }

        String name =  UUID.randomUUID().toString() + ".jpeg";

        MultipartFile file = base64toMultipart(imgBase, name);
        String path = ROOT + "/" + user.getUserId() + "/image/head/";
        String pathName = path + name;
        File dest = new File(path);
        //判断文件父目录是否存在
        if(!dest.exists()){
            dest.mkdirs();
        }
        try {
            // 保存图片
            Files.copy(file.getInputStream(), Paths.get(path, name));
            userService.updateUserHeadUrlById(user.getId(), pathName);
            user.setHeadUrl(pathName);
            request.getSession().setAttribute("user", user);
            return new FileDTO(1, "上传成功" , pathName);
        } catch (Exception e) {
            e.printStackTrace();
            return new FileDTO(0,"上传失败，请重试", "");
        }

    }


    @RequestMapping(method = RequestMethod.GET, value = "/file/{userId}/image/head/{filename:.+}", produces = "image/png")
    @ResponseBody
    public ResponseEntity<?> getFile(@PathVariable("filename") String filename,
                                     @PathVariable("userId") String userId) {
        try {
            String path = ROOT + "/" + userId + "/image/head/";
            return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(path, filename)));
        } catch (Exception e) {
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
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] b = decoder.decodeBuffer(baseStrs[1]);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            return new Base64MultipartFile(b, baseStrs[0], fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
