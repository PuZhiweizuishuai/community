package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.LogDTO;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.LogService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.FileUtil;
import com.buguagaoshu.community.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-07 22:41
 * 管理员控制日志的接口
 */
@Controller
@Slf4j
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

    private final UserService userService;


    @Autowired
    public AdminLogApiController(LogService logService, UserService userService) {
        this.logService = logService;
        this.userService = userService;
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

    @PostMapping("/admin/log/delete")
    @ResponseBody
    public Map<String, Object> deleteLog(@RequestBody LogDTO logDTO, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("admin");
        if (user == null) {
            return StringUtil.dealResultMessage(false, "权限不足！");
        }
        user = userService.selectUserById(user.getId());
        if (StringUtil.judgePassword(logDTO.getPassword(), user.getPassword())) {
            if (FileUtil.checkLogDelete(logDTO.getTitle())) {
                if (logService.deleteLog(logDTO.getTitle())) {
                    log.error("管理员 {} 删除了日志 {}", user.getId(), logDTO.getTitle());
                    return StringUtil.dealResultMessage(true, "删除成功");
                } else {
                    return StringUtil.dealResultMessage(false, "未发现此文件");
                }
            } else {
                return StringUtil.dealResultMessage(false, "不支持格式，或非法字符！");
            }
        } else {
            return StringUtil.dealResultMessage(false, "密码错误");
        }
    }

    private void setHttpHead(HttpHeaders headers, String filename, String contentDisposition) {
        if (FileUtil.checkLogShow(filename)) {
            headers.add(HttpHeaders.ACCEPT, "text/**");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/**; charset=utf-8");
        } else {
            headers.add(HttpHeaders.ACCEPT, "application/x-zip-compressed");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/x-zip-compressed; charset=utf-8");
        }
    }
}

