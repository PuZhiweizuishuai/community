package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.cache.HotQuestionCache;
import com.buguagaoshu.community.cache.HotTagCache;
import com.buguagaoshu.community.cache.HotUserCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-15 13:47
 */
@Controller
public class UserListController {
    private final UserService userService;

    private final HotUserCache hotUserCache;

    private final HotTagCache hotTagCache;

    private final HotQuestionCache hotQuestionCache;

    @Autowired
    public UserListController(UserService userService, HotUserCache hotUserCache, HotTagCache hotTagCache, HotQuestionCache hotQuestionCache) {
        this.userService = userService;
        this.hotUserCache = hotUserCache;
        this.hotTagCache = hotTagCache;
        this.hotQuestionCache = hotQuestionCache;
    }


    /**
     * @param type 翻页类型
     *             type=0 默认翻页新用户
     *             type=1 翻页活跃用户
     * */
    @GetMapping("/user")
    public String getUserListPage(Model model,
                                  @RequestParam(value = "page", defaultValue = "1") String page,
                                  @RequestParam(value = "size", defaultValue = "10") String size,
                                  @RequestParam(value = "class", defaultValue = "0") String type) {
        if(type.equals("0")) {
            model.addAttribute("hotUser", hotUserCache.getUserDtoList());
        }
        List<String> hots = hotTagCache.getHots();
        PaginationDto<User> userPaginationDto = userService.getUserList(page, size);
        model.addAttribute("hotQuestions", hotQuestionCache.getHotQuestionDTOList());
        model.addAttribute("newUser", userPaginationDto);
        model.addAttribute("hots", hots);
        model.addAttribute("class", type);
        model.addAttribute("page", page);
        return "userList";
    }
}
