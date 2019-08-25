package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.service.SignInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 21:59
 * 登陆控制API
 */
@RestController
public class SignInApiController {
    private final SignInService signInService;

    @Autowired
    public SignInApiController(SignInService signInService) {
        this.signInService = signInService;
    }


    /**
     * 登陆接口
     * */
    @PostMapping("/api/signIn")
    @ResponseBody
    public HashMap<String, Object> judgeSignIn(String email, String password,
                                               String remember, String captcha,
                                               HttpServletRequest request) {
        try {
            return signInService.signIn(email, password, remember, captcha, request);
        } catch (CustomizeException e) {
            HashMap<String, Object> hashMap = new HashMap<>(2);
            hashMap.put("success", false);
            hashMap.put("msg", ResultDTO.errorOf(e));
            return  hashMap;
        }
    }
}
