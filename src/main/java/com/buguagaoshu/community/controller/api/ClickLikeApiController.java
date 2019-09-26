package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ClickLikeDTO;
import com.buguagaoshu.community.enums.ClickLikeTypeEnum;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ClickLikeService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.JwtUtil;
import com.buguagaoshu.community.util.StringUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-25 17:01
 */
@RestController
public class ClickLikeApiController {
    private final ClickLikeService clickLikeService;

    private final UserService userService;

    private final JwtUtil jwtUtil;

    @Autowired
    public ClickLikeApiController(ClickLikeService clickLikeService, JwtUtil jwtUtil, UserService userService) {
        this.clickLikeService = clickLikeService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/api/clickLike")
    public Map<String, Object> clickLike(@RequestBody ClickLikeDTO clickLikeDTO) {
        if (clickLikeDTO.getToken() == null || clickLikeDTO.getToken().equals("")) {
            return StringUtil.dealResultMessage(false, "请先登陆后再点赞!");
        }
        try {
            Claims claims = jwtUtil.parseJWT(clickLikeDTO.getToken());
            clickLikeDTO.setNotifierName(claims.get("userName").toString());

            if (clickLikeDTO.getReceiver().equals(Long.valueOf(claims.getId()))) {
                return StringUtil.dealResultMessage(false, "不能给自己点赞！");
            }
            ClickLikeTypeEnum clickLikeTypeEnum = clickLikeService.createClickLike(clickLikeDTO);
            if (clickLikeTypeEnum.equals(ClickLikeTypeEnum.SUCCESS)) {
                return StringUtil.dealResultMessage(true, "点赞成功！");
            }  else if(clickLikeTypeEnum.equals(ClickLikeTypeEnum.SUCCESS_CANCEL)) {
                return StringUtil.dealResultMessage(true, clickLikeTypeEnum.getName());
            } else {
                return StringUtil.dealResultMessage(false, clickLikeTypeEnum.getName());
            }
        } catch (Exception e) {
            return StringUtil.dealResultMessage(false, "请先登陆后再点赞!");
        }
    }


    @PostMapping("/api/test/clickLike")
    public ClickLikeDTO testClick(@RequestBody ClickLikeDTO clickLikeDTO) {
        System.out.println(clickLikeDTO);
        return clickLikeDTO;
    }
}
