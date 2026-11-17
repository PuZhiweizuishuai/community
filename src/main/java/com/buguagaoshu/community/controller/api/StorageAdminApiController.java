package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.model.StorageConfig;
import com.buguagaoshu.community.repository.StorageConfigRepository;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台「存储配置」：本地存储启停 + 多对象存储 CRUD / 连接测试。
 * secretKey 只写不读，列表仅返回是否已配置。
 */
@RestController
@RequestMapping("/api/admin/storage")
public class StorageAdminApiController {

    public record SaveRequest(Long id, String name, String endpoint, String region, String accessKey,
                              String secretKey, String bucket, Boolean pathStyleAccess,
                              Long urlExpireSeconds, Boolean enabled) {
    }

    public record IdRequest(long id) {
    }

    private final StorageConfigRepository storageConfigRepository;

    public StorageAdminApiController(StorageConfigRepository storageConfigRepository) {
        this.storageConfigRepository = storageConfigRepository;
    }

    @GetMapping
    public ResultDTO<List<Map<String, Object>>> list(HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        List<Map<String, Object>> views = storageConfigRepository.list().stream().map(config -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", config.getId());
            view.put("name", config.getName());
            view.put("type", config.getType());
            view.put("endpoint", config.getEndpoint());
            view.put("region", config.getRegion());
            view.put("accessKey", config.getAccessKey());
            view.put("secretSet", StringUtils.isNotBlank(config.getSecretKey()));
            view.put("bucket", config.getBucket());
            view.put("pathStyleAccess", config.getPathStyleAccess());
            view.put("urlExpireSeconds", config.getUrlExpireSeconds());
            view.put("enabled", config.getEnabled());
            view.put("fileCount", storageConfigRepository.fileCount(config.getId()));
            view.put("createTime", config.getCreateTime());
            view.put("modifiedTime", config.getModifiedTime());
            return view;
        }).toList();
        return ResultDTO.okOf(views);
    }

    @PostMapping("/save")
    public ResultDTO<Long> save(@RequestBody SaveRequest body, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        StorageConfig saved = storageConfigRepository.save(toConfig(body));
        return ResultDTO.okOf(saved.getId());
    }

    @PostMapping("/delete")
    public ResultDTO<Object> delete(@RequestBody IdRequest body, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        storageConfigRepository.delete(body.id());
        return ResultDTO.okOf();
    }

    @PostMapping("/test")
    public ResultDTO<Object> test(@RequestBody SaveRequest body, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        storageConfigRepository.test(toConfig(body));
        return ResultDTO.okOf();
    }

    private StorageConfig toConfig(SaveRequest body) {
        StorageConfig config = new StorageConfig();
        config.setId(body.id());
        config.setName(body.name());
        config.setEndpoint(body.endpoint());
        config.setRegion(body.region());
        config.setAccessKey(body.accessKey());
        config.setSecretKey(body.secretKey());
        config.setBucket(body.bucket());
        config.setPathStyleAccess(body.pathStyleAccess());
        config.setUrlExpireSeconds(body.urlExpireSeconds());
        config.setEnabled(body.enabled());
        return config;
    }
}