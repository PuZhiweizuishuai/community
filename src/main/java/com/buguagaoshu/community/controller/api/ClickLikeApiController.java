package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ClickLikeDTO;
import com.buguagaoshu.community.enums.ClickLikeTypeEnum;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ClickLikeService;
import com.buguagaoshu.community.util.StringUtil;
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

    @Autowired
    public ClickLikeApiController(ClickLikeService clickLikeService) {
        this.clickLikeService = clickLikeService;
    }

    @PostMapping("/api/clickLike")
    public Map<String, Object> clickLike(@RequestBody ClickLikeDTO clickLikeDTO, HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        if(user == null) {
            return StringUtil.dealResultMessage(false, "请先登陆后再点赞!");
        }
        if(clickLikeDTO.getReceiver().equals(clickLikeDTO.getNotifier())) {
            return StringUtil.dealResultMessage(false, "不能给自己点赞！");
        }
        clickLikeDTO.setNotifierName(user.getUserName());
        ClickLikeTypeEnum clickLikeTypeEnum = clickLikeService.createClickLike(clickLikeDTO);
        if(clickLikeTypeEnum.equals(ClickLikeTypeEnum.SUCCESS)) {
            return StringUtil.dealResultMessage(true, "点赞成功！");
        } else {
            return StringUtil.dealResultMessage(false, clickLikeTypeEnum.getName());
        }
    }


    @PostMapping("/api/test/clickLike")
    public ClickLikeDTO testClick(@RequestBody ClickLikeDTO clickLikeDTO) {
        System.out.println(clickLikeDTO);
        return clickLikeDTO;
    }
}
