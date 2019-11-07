package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.LogService;
import com.buguagaoshu.community.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-07 22:41
 * 管理员控制日志的接口
 */
@Controller
class AdminLogApiController {
    /**
     * 下载日志
     */
    private final String DOWNLOAD_TYPE = "download";

    /**
     * 显示日志
     */
    private final String SHOW_TYPE = "show";

    @Value("${File.LOG.PATH}")
    private String LOG_PATH;

    private final LogService logService;


    @Autowired
    public AdminLogApiController(LogService logService) {
        this.logService = logService;
    }


    @GetMapping("/admin/log/download/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Object> downloadLog(@PathVariable("filename") String filename,
                                              @RequestParam(value = "type", defaultValue = "download") String type,
                                              HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("admin");
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        switch (type) {
            case DOWNLOAD_TYPE:
                setHttpHead(headers, filename, "attachment;filename=\"");
                break;
            case SHOW_TYPE:
                setHttpHead(headers, filename, "inline;filename=\"");
                break;
            default:
                return ResponseEntity.notFound().build();
        }
        Resource file = logService.loadLogForDown(filename);
        return new ResponseEntity<Object>(file, headers, HttpStatus.OK);
    }

    public Map<String, Object> deleteLog() {
        return null;
    }

    private void setHttpHead(HttpHeaders headers, String filename, String contentDisposition) {
        if (FileUtil.checkLogShow(filename)) {
            headers.add(HttpHeaders.ACCEPT, "text/**");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/**");
        } else {
            headers.add(HttpHeaders.ACCEPT, "application/x-zip-compressed");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/x-zip-compressed");
        }
    }
}

