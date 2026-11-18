package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.FileDTO;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.FileRecord;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.repository.FileRepository;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一文件接口：上传 POST /api/file/upload，读取 GET /api/file/{storageId}/{key}。
 * 不再区分 image/video/file 类型，storageId=0 为本地存储，>=1 为对象存储。
 * 业务逻辑在 FileRepository，这里只做协议适配（editormd / Vditor 两种上传响应）。
 */
@RestController
@RequestMapping("/api/file")
@Slf4j
public class FileApiController {

    private static final int MAX_BATCH_SIZE = 9;

    private final FileRepository fileRepository;
    private final UserMapper userMapper;

    public FileApiController(FileRepository fileRepository, UserMapper userMapper) {
        this.fileRepository = fileRepository;
        this.userMapper = userMapper;
    }

    /**
     * 统一上传入口，兼容三种表单字段：
     * editormd-image-file（editor.md 单图）、file[]（Vditor 批量）、file（通用单文件）。
     * type=top 时同时把文件设为用户主页封面。
     */
    @PostMapping("/upload")
    @RequirePermission(PermissionCodes.FILE_UPLOAD)
    public Object upload(@RequestParam(value = "editormd-image-file", required = false) MultipartFile editorFile,
                         @RequestParam(value = "file[]", required = false) MultipartFile[] vditorFiles,
                         @RequestParam(value = "file", required = false) MultipartFile singleFile,
                         @RequestParam(value = "type", required = false) String type,
                         HttpServletRequest request) {
        boolean editorMd = editorFile != null;
        // 管理员与普通用户共用 session.user，后台身份由 RBAC 权限判定
        User user = AuthUtil.currentUser(request);
        if (user == null) {
            user = AuthUtil.currentAdmin(request);
        }
        if (user == null) {
            return uploadError(editorMd, "未登录用户禁止上传文件");
        }

        List<MultipartFile> files = new ArrayList<>();
        if (editorFile != null) {
            files.add(editorFile);
        }
        if (vditorFiles != null) {
            files.addAll(List.of(vditorFiles));
        }
        if (singleFile != null) {
            files.add(singleFile);
        }
        if (files.isEmpty()) {
            return uploadError(editorMd, "没有收到任何文件");
        }
        if (files.size() > MAX_BATCH_SIZE) {
            return uploadError(editorMd, "一次最多只能上传" + MAX_BATCH_SIZE + "个文件，你已超过了这个数量");
        }

        try {
            // 先整体校验再保存，避免批量上传中途失败留下半截文件
            files.forEach(fileRepository::validate);
            Map<String, String> succMap = new LinkedHashMap<>();
            String firstUrl = null;
            for (MultipartFile file : files) {
                FileRecord record = fileRepository.upload(file, user);
                succMap.put(file.getOriginalFilename(), record.getUrl());
                if (firstUrl == null) {
                    firstUrl = record.getUrl();
                }
            }
            if ("top".equals(type) && AuthUtil.currentUser(request) != null && firstUrl != null) {
                userMapper.updateUserTopPhotoUrl(user.getId(), firstUrl);
                user.setUserTopPhotoUrl(firstUrl);
                request.getSession().setAttribute("user", user);
            }
            if (editorMd) {
                return new FileDTO(1, "success", firstUrl);
            }
            // Vditor 约定 code == 0 表示成功，非 0 会把 msg 当作错误提示
            Map<String, Object> data = new LinkedHashMap<>(2);
            data.put("errFiles", new String[0]);
            data.put("succMap", succMap);
            Map<String, Object> result = new LinkedHashMap<>(3);
            result.put("msg", "上传成功！");
            result.put("code", 0);
            result.put("data", data);
            return result;
        } catch (CustomizeException e) {
            return uploadError(editorMd, e.getMessage());
        }
    }

    /**
     * 统一读取：本地存储流式输出，对象存储 302 到预签名 URL。
     * key 为存储内相对路径，如 MJM/image/xxx.jpg、MJM/image/head/xxx.jpeg。
     */
    @GetMapping("/{storageId}/{*key}")
    public ResponseEntity<?> read(@PathVariable("storageId") long storageId,
                                  @PathVariable("key") String key) {
        try {
            FileRepository.StoredFile file = fileRepository.resolve(storageId, key);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            if (file.redirectUrl() != null) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(file.redirectUrl()))
                        .build();
            }
            return localResponse(file);
        } catch (Exception e) {
            log.error("文件获取失败, storageId={}, key={}: {}", storageId, key, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Resource> localResponse(FileRepository.StoredFile file) {
        String contentType = file.contentType();
        boolean inline = contentType.startsWith("image/") || contentType.startsWith("video/")
                || contentType.startsWith("audio/") || "application/pdf".equals(contentType);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        headers.setContentDisposition((inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.downloadName(), StandardCharsets.UTF_8)
                .build());
        // 文件名含 UUID，内容不可变，允许浏览器长期缓存
        headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        return new ResponseEntity<>(file.resource(), headers, HttpStatus.OK);
    }

    private Object uploadError(boolean editorMd, String message) {
        if (editorMd) {
            return new FileDTO(0, message, "");
        }
        Map<String, Object> result = new LinkedHashMap<>(2);
        result.put("msg", message);
        result.put("code", 1);
        return result;
    }
}