package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-04 20:35
 * 问题搜索的简单实现
 */
@Controller
public class SearchController {

    private final QuestionService questionService;

    private final UserService userService;

    @Autowired
    public SearchController(QuestionService questionService, UserService userService) {
        this.questionService = questionService;
        this.userService = userService;
    }

    @GetMapping("/search/{search}")
    public String getSearchQuestionPage(@PathVariable(name = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "1") String page,
                                @RequestParam(value = "size", defaultValue = "10") String size,
                                Model model) {
        if(StringUtils.isBlank(search)) {
            return "search";
        }
        String tempSearch = search;
        String[] searchs = search.split(" ");
        search = Arrays
                .stream(searchs)
                .filter(StringUtils::isNotBlank)
                .map(t -> t.replace("+", "").replace("*", "").replace("?", ""))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("|"));


        PaginationDto<QuestionDto> paginationDto = questionService.searchQuestion(search, page, size);
        PaginationDto<User> userpaginationDto = userService.searchUser(search, page, "5");
        paginationDto.setSearch(tempSearch);
        model.addAttribute("paginationDto", paginationDto);
        model.addAttribute("userpaginationDto", userpaginationDto);
        return "search";
    }

    @GetMapping("/search/{search}/user")
    public String getSearchUserPage(@PathVariable(name = "search", required = false) String search,
                                    @RequestParam(value = "page", defaultValue = "1") String page,
                                    @RequestParam(value = "size", defaultValue = "10") String size,
                                    Model model) {
        if(StringUtils.isBlank(search)) {
            return "searchUser";
        }
        String tempSearch = search;
        String[] searchs = search.split(" ");

        search = Arrays
                .stream(searchs)
                .filter(StringUtils::isNotBlank)
                .map(t -> t.replace("+", "").replace("*", "").replace("?", ""))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("|"));

        PaginationDto<User> paginationDto = userService.searchUser(search, page, size);
        paginationDto.setSearch(tempSearch);
        model.addAttribute("paginationDto", paginationDto);
        return "searchUser";
    }
}
