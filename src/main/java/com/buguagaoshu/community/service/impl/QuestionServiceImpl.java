package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:51
 */
@Service
public class QuestionServiceImpl implements QuestionService {
    private final QuestionMapper questionMapper;

    private final UserService userService;

    @Autowired
    public QuestionServiceImpl(QuestionMapper questionMapper, UserService userService) {
        this.questionMapper = questionMapper;
        this.userService = userService;
    }


    @Override
    public int createQuestion(Question question) {
        return questionMapper.createQuestion(question);
    }

    /**
     * TODO 优化查询，考虑使用多表级联
     * */
    @Override
    public PaginationDto getSomeQuestionDto(String page, String size) {
        int offset;
        int sizeNumber;
        // 使用 string 防止传入参数不是数字
        try {
            offset = Integer.valueOf(page);
            sizeNumber = Integer.valueOf(size);
        } catch (Exception e) {
            offset = 1;
            sizeNumber = 10;
        }
        // 防止传入参数为负
        if(offset <= 0) {
            offset = 1;
        }
        // 控制页面显示问题数量
        if(sizeNumber > 20 || sizeNumber < 5) {
            sizeNumber = 10;
        }
        int allQuestionCount = questionMapper.getQuestionCount();
        int totalPage = 1;

        // 计算总页数
        if (allQuestionCount % sizeNumber == 0) {
            totalPage = allQuestionCount / sizeNumber;
        } else {
            totalPage = (allQuestionCount / sizeNumber) + 1;
        }
        // 容错处理，防止用户手动输入过大的数
        if(offset >= totalPage) {
            offset = totalPage;
        }


        // 计算分页公式 size * (page - 1)
        int pageParam = sizeNumber * (offset - 1);
        List<Question> questionList = questionMapper.getSomeQuestion(pageParam, sizeNumber);
        List<QuestionDto> questionDtoList = new ArrayList<>();
        for(Question question : questionList) {
            User user = userService.selectUserById(question.getUserId());
            // TODO 此处只需要头像地址就好，后期需要优化
            // 保护隐私
            user.clean();
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setUser(user);
            questionDtoList.add(questionDto);
        }

        PaginationDto paginationDto = new PaginationDto();
        paginationDto.setQuestions(questionDtoList);
        paginationDto.setPagination(totalPage, offset, sizeNumber);
        return paginationDto;
    }
}
