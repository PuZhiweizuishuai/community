package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.AdminDataDTO;
import com.buguagaoshu.community.dto.AdminPageDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.mapper.AdminDataMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.AdminData;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdminDataService;
import com.buguagaoshu.community.service.OnlineUserService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 19:02
 */
@Controller
public class AdminController {
    private AdminPageDto adminPageDto = new AdminPageDto();

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
        adminPageDto.setUserNumber(userService.getAlluserCount());
        adminPageDto.setQuestionNumber(questionService.getAllQuestionCount());
        adminPageDto.setOnlineUser(onlineUserService.selectOnlineUserCount());
    }

    @GetMapping("/admin")
    public String getAdminPage() {
        return "/admin/index";
    }


    @GetMapping("/admin/main")
    public String getIndexPage(@RequestParam(value = "page", defaultValue = "1") String page,
                               @RequestParam(value = "size", defaultValue = "10") String size,
                               Model model) {
        PaginationDto<AdminDataDTO> paginationDto = adminDataService.selectAdminData(page, size);
        model.addAttribute(adminPageDto);
        model.addAttribute("paginationDto", paginationDto);
        return "/admin/main";
    }

    @GetMapping("/admin/user")
    public String getAdminUserPage(@RequestParam(value = "page", defaultValue = "1") String page,
                                   @RequestParam(value = "size", defaultValue = "10") String size,
                                   Model model) {
        PaginationDto<User> paginationDto = userService.getUserList(page, size);
        model.addAttribute(adminPageDto);
        model.addAttribute("paginationDto", paginationDto);
        return "/admin/user";
    }

    @GetMapping("/admin/question")
    public String getAdminQuestionPage(@RequestParam(value = "page", defaultValue = "1") String page,
                                       @RequestParam(value = "size", defaultValue = "10") String size,
                                       Model model) {
        PaginationDto<QuestionDto> paginationDto = questionService.getAllQuestionList(page, size);
        model.addAttribute(adminPageDto);
        model.addAttribute("paginationDto", paginationDto);
        return "/admin/question";
    }
}
