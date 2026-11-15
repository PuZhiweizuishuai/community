package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ClickLikeDTO;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.enums.ClickLikeTypeEnum;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ClickLikeService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 点赞 API（问题或评论）。身份取自会话，不再需要前端传 token。
 */
@RestController
@Slf4j
public class ClickLikeApiController {

    private final ClickLikeService clickLikeService;

    public ClickLikeApiController(ClickLikeService clickLikeService) {
        this.clickLikeService = clickLikeService;
    }

    @PostMapping({"/api/like", "/api/clickLike"})
    public ResultDTO<Map<String, Object>> clickLike(@RequestBody ClickLikeDTO body,
                                                    HttpServletRequest request) {
        User user = AuthUtil.requireUser(request);
        body.setNotifier(user.getId());
        body.setNotifierName(user.getUserName());
        if (body.getCommentId() == null) {
            body.setCommentId(-1L);
        }
        ClickLikeTypeEnum type = clickLikeService.createClickLike(body);
        if (type == ClickLikeTypeEnum.SUCCESS || type == ClickLikeTypeEnum.SUCCESS_CANCEL) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("liked", type == ClickLikeTypeEnum.SUCCESS);
            data.put("message", type.getName());
            return ResultDTO.okOf(data);
        }
        return ResultDTO.errorOf(2019, type.getName());
    }
}
