package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.AdminDataDTO;
import com.buguagaoshu.community.dto.AdminPageDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdminDataService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 19:02
 */
@Controller
public class AdminController {
    final
    AdminDataService adminDataService;

    final
    QuestionService questionService;


    final
    UserService userService;


    final
    OnlineUserService onlineUserService;

    @Autowired
    public AdminController(QuestionService questionService, UserService userService, OnlineUserService onlineUserService, AdminDataService adminDataService) {
        this.questionService = questionService;
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.adminDataService = adminDataService;

    }

    @GetMapping("/admin")
    public String getAdminPage() {
        return "/admin/index";
    }


    @GetMapping("/admin/main")
    public String getIndexPage(@RequestParam(value = "page", defaultValue = "1") String page,
                               @RequestParam(value = "size", defaultValue = "10") String size,
                               Model model) {
        AdminPageDto adminPageDto = new AdminPageDto();
        adminPageDto.setUserNumber(userService.getAlluserCount());
        adminPageDto.setQuestionNumber(questionService.getAllQuestionCount());
        adminPageDto.setOnlineUser(onlineUserService.selectOnlineUserCount());
        PaginationDto<AdminDataDTO> paginationDto = adminDataService.selectAdminData(page, size);
        model.addAttribute(adminPageDto);
        System.out.println(adminPageDto.getOnlineUser());
        model.addAttribute("paginationDto", paginationDto);
        return "/admin/main";
    }

    @GetMapping("/admin/user")
    public String getAdminUserPage(@RequestParam(value = "page", defaultValue = "1") String page,
                                   @RequestParam(value = "size", defaultValue = "10") String size,
                                   Model model) {
        PaginationDto<User> paginationDto = userService.getUserList(page, size);
        model.addAttribute("paginationDto", paginationDto);
        return "/admin/user";
    }

    @GetMapping("/admin/question")
    public String getAdminQuestionPage(@RequestParam(value = "page", defaultValue = "1") String page,
                                       @RequestParam(value = "size", defaultValue = "10") String size,
                                       @RequestParam(value = "class", required = false) String classification,
                                       Model model) {
        // TODO 等待添加分类查找
        PaginationDto<QuestionDto> paginationDto = questionService.getAllQuestionList(page, size);
        model.addAttribute("paginationDto", paginationDto);
        return "/admin/question";
    }

    @GetMapping("/admin/signOut")
    public String adminSignOut(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("admin");
        if(user != null) {
            request.getSession().removeAttribute("admin");
            return "redirect:/admin/index";
        }
        return "redirect:/admin/index";
    }

    @GetMapping("/admin/search")
    public String search(@RequestParam(value = "page", defaultValue = "1") String page,
                         @RequestParam(value = "type" , defaultValue = "question") String type,
                         @RequestParam(value = "size", defaultValue = "10") String size,
                         @RequestParam(value = "search", required = false) String search,
                         Model model) {
        PaginationDto<QuestionDto> questionDtoPaginationDto = null;
        PaginationDto<User> userPaginationDto = null;
        if(StringUtils.isBlank(search)) {
            model.addAttribute("search", search);
            model.addAttribute("type", type);
            model.addAttribute("questions", questionDtoPaginationDto);
            model.addAttribute("users", userPaginationDto);
            return "/admin/search";
        }
        String tempSearch = search;
        String[] searchs = search.split(" ");

        search = Arrays
                .stream(searchs)
                .filter(StringUtils::isNotBlank)
                .map(t -> t.replace("+", "").replace("*", "").replace("?", ""))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("|"));

        if(type.equals("question")) {
            questionDtoPaginationDto = questionService.searchAllQuestionList(search, page, size);
            model.addAttribute("questions", questionDtoPaginationDto);
        } else {
            userPaginationDto = userService.searchUser(search, page, size);
            model.addAttribute("users", userPaginationDto);
        }

        model.addAttribute("search", search);
        model.addAttribute("type", type);
        return "/admin/search";
    }
}
