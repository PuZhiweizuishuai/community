package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.ResultDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 纯 API 后端的根路径提示。页面由 web/ 下的 Vue3 应用提供。
 */
@RestController
public class RootApiController {

    @GetMapping("/")
    public ResultDTO<Map<String, Object>> index() {
        return ResultDTO.okOf(Map.of(
                "name", "community-api",
                "description", "这是前后端分离后的 REST API 服务，页面请访问 Vue 前端（默认 http://localhost:5173）",
                "meta", "/api/meta",
                "session", "/api/auth/session"));
    }
}
