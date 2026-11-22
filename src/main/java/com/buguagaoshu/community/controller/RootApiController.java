package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.ResultDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 根路径返回打包后的 Vue 页面；API 自描述信息移到 /api/info。
 *
 * <p>注意：这里不能再用 @RestController + @GetMapping("/") 返回 JSON，
 * 否则会覆盖 Spring Boot 对 static/index.html 的欢迎页映射。
 */
@Controller
public class RootApiController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/api/info")
    @ResponseBody
    public ResultDTO<Map<String, Object>> info() {
        return ResultDTO.okOf(Map.of(
                "name", "community-api",
                "description", "社区 REST API；页面由 static/index.html 提供",
                "meta", "/api/meta",
                "session", "/api/auth/session"));
    }
}
