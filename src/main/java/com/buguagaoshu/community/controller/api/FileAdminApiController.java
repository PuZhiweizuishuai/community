package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.FileRecordDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.repository.FileRepository;
import com.buguagaoshu.community.util.AuthUtil;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理后台「文件管理」：全量文件分页查询与删除。
 */
@RestController
@RequestMapping("/api/admin/file")
public class FileAdminApiController {

    public record IdRequest(long id) {
    }

    private final FileRepository fileRepository;

    public FileAdminApiController(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @GetMapping("/list")
    public ResultDTO<Map<String, Object>> list(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "20") long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "storageId", required = false) Long storageId,
            @RequestParam(value = "status", required = false) Integer status,
            HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        Page<FileRecordDto> result = fileRepository.list(Math.max(page, 1), Math.min(Math.max(size, 1), 100),
                keyword, fileType, storageId, status);
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("list", result.getRecords());
        data.put("total", result.getTotalRow());
        data.put("page", result.getPageNumber());
        data.put("size", result.getPageSize());
        return ResultDTO.okOf(data);
    }

    @PostMapping("/delete")
    public ResultDTO<Object> delete(@RequestBody IdRequest body, HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        fileRepository.deleteById(body.id());
        return ResultDTO.okOf();
    }
}