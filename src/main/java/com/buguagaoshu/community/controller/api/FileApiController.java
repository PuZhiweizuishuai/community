package com.buguagaoshu.community.controller.api;

import com.alibaba.fastjson.JSONObject;
import com.buguagaoshu.community.dto.FileDTO;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.util.FileUtil;
import com.buguagaoshu.community.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 18:43
 * 文件上传
 */
@RestController
@Slf4j
public class FileApiController {
    @Value("${File.ROOT.PATH}")
    private String ROOT;

    private final ResourceLoader resourceLoader;

    private final UserMapper userMapper;


    @Autowired
    public FileApiController(ResourceLoader resourceLoader, UserMapper userMapper) {
        this.resourceLoader = resourceLoader;
        this.userMapper = userMapper;
    }

    @PostMapping(value = "/api/file/image/upload")
    public Object upload(@RequestParam(value = "editormd-image-file", required = false) MultipartFile file, String type,
                         HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return new FileDTO(0, "未登陆用户禁止上传图片","");
        }
        String fileName = file.getOriginalFilename();
        if(!ImageUtil.checkFileType(fileName)) {
            return new FileDTO(0, "图片格式错误", "");
        }

        String name = ImageUtil.createNewFileName(fileName);
        String path = ROOT + "/" + user.getUserId() + "/image/";
        String pathName = "/" + path + name;
        File dest = new File(path);
        //判断文件父目录是否存在
        if(!dest.exists() && !dest.mkdirs()){
            return new FileDTO(0,"上传失败，请重试", "");
        }

        // TODO 思考如何标记未使用图片
        try {
            // 保存图片
            Files.copy(file.getInputStream(), Paths.get(path, name));

            if(type != null && type.equals("top")) {
                userMapper.updateUserTopPhotoUrl(user.getId(), pathName);
                user.setUserTopPhotoUrl(pathName);
                request.getSession().setAttribute("user", user);
            }

            JSONObject res = new JSONObject();
            res.put("url", pathName);
            res.put("success", 1);
            res.put("message", "success");
            return res;
        } catch (Exception e) {
            log.error("文件保存失败：  {}",e.getMessage());
            return new FileDTO(0,"上传失败，请重试", "");
        }
    }


    @PostMapping(value = "/api/file/upload")
    public Map<String, Object> newUpload(@RequestParam(value = "file[]", required = false) MultipartFile[] files,
                                         HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("msg", "你还没有登陆，请登陆后重试！");
        map.put("code", 1);
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return map;
        }
        if(files.length > 9) {
            map.put("msg", "一次最多只能上传9个文件，你已超过了这个数量！");
            return map;
        }

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (!FileUtil.checkFileType(fileName)) {
                map.put("msg", "不支持的文件格式！");
                return map;
            }
        }

        Map<String, Object> succMap = new HashMap<>(2);
        Map<String, Object> data = new HashMap<>(2);
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            String name = ImageUtil.createNewFileName(fileName);
            String path = ROOT + "/" + user.getUserId() + "/file/";
            if (ImageUtil.checkFileType(fileName)) {
                path = ROOT + "/" + user.getUserId() + "/image/";
            }
            String pathName = "/" + path + name;
            File dest = new File(path);
            //判断文件父目录是否存在
            if (!dest.exists() && !dest.mkdirs()) {
                map.put("msg", "上传失败，请重试");
                return map;
            }
            try {
                Files.copy(file.getInputStream(), Paths.get(path, name));
                succMap.put(fileName, pathName);
            } catch (Exception e) {
                map.put("msg", "上传失败，请重试");
                return map;
            }
        }
        map.put("msg", "上传成功！");
        data.put("succMap", succMap);
        map.put("data", data);
        return map;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/file/{userId}/image/{filename:.+}", produces = "image/png")
    @ResponseBody
    public ResponseEntity<?> getImage(@PathVariable("filename") String filename,
                                     @PathVariable("userId") String userId) {
        try {
            String path = ROOT + "/" + userId + "/image/";
            return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(path, filename)));
        } catch (Exception e) {
            log.error("图片文件获取失败:  {}",e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/file/{userId}/video/{filename:.+}", produces = "video/mp4")
    @ResponseBody
    public ResponseEntity<?> getVideo(@PathVariable("filename") String filename,
                                     @PathVariable("userId") String userId) {
        try {
            String path = ROOT + "/" + userId + "/video/";
            return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(path, filename)));
        } catch (Exception e) {
            log.error("视频文件获取失败:  {}",e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/file/{userId}/file/{filename:.+}")
    @ResponseBody
    public ResponseEntity<?> getFile(@PathVariable("filename") String filename,
                                      @PathVariable("userId") String userId) {
        HttpHeaders headers = new HttpHeaders();

        switch (FileUtil.getFileType(filename)) {
            case IMAGE_FILE:
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "filename=" + filename);
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE);
                headers.add(HttpHeaders.ACCEPT, MediaType.IMAGE_PNG_VALUE);
                break;
            case MUSIC_FILE:
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "filename=" + filename);
                headers.add(HttpHeaders.CONTENT_TYPE, "audio/mp3");
                headers.add(HttpHeaders.ACCEPT, "audio/mp3");
                break;
            case VIDEO_FILE:
                headers.add(HttpHeaders.ACCEPT, "video/mp4");
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "filename=" + filename);
                headers.add(HttpHeaders.CONTENT_TYPE, "video/mp4");
                break;
            case PDF_FILE:
                headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_PDF_VALUE);
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "filename=" + filename);
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);
                break;
            default:
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "filename=" + filename);
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
        }
        try {
            String path = ROOT + "/" + userId + "/file/";
            return new ResponseEntity<Object>(resourceLoader.getResource("file:" + Paths.get(path, filename)), headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("文件获取失败:  {}",e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
