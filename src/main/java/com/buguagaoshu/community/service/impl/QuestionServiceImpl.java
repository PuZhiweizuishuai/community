package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;
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


    private int[] getPageAndsize(String page, String size, long id) {
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

        // 控制页面显示问题数量
        if(sizeNumber > 20 || sizeNumber < 5) {
            sizeNumber = 10;
        }
        int allQuestionCount;
        if(id == -1) {
            allQuestionCount = questionMapper.getQuestionCount();
        } else {
            allQuestionCount = questionMapper.getUserQuestionCount(id);
        }

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
        // 防止传入参数为负
        if(offset <= 0) {
            offset = 1;
        }


        // 计算分页公式 size * (page - 1)
        int pageParam = sizeNumber * (offset - 1);
        int[] param = new int[4];
        // 页码
        param[0] = pageParam;
        // 每页显示数
        param[1] = sizeNumber;
        // 总页数
        param[2] = totalPage;
        // 当前页
        param[3] = offset;
        return param;
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
        int[] param = getPageAndsize(page, size, -1);
        List<Question> questionList = questionMapper.getSomeQuestion(param[0], param[1]);
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
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;
    }

    @Override
    public PaginationDto getQuestionByUserId(String page, String size, long id) {
        int[] param = getPageAndsize(page, size, id);
        List<Question> questionList = questionMapper.getQuestionByUserId(param[0], param[1], id);
        List<QuestionDto> questionDtoList = new ArrayList<>();
        for(Question question : questionList) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDtoList.add(questionDto);
        }
        PaginationDto paginationDto = new PaginationDto();
        paginationDto.setQuestions(questionDtoList);
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;

    }

    @Override
    public QuestionDto selectQuestionById(String questionId) {
        long id;
        try {
            id = Long.valueOf(questionId);
        } catch (Exception e) {
            id = -1;
        }
        if(id == -1) {
            return null;
        }

        Question question = questionMapper.selectQuestionById(id);
        if(question == null) {
            return null;
        }
        User user = userService.selectUserById(question.getUserId());
        user.clean();
        QuestionDto questionDto = new QuestionDto();
        BeanUtils.copyProperties(question, questionDto);
        questionDto.setUser(user);
        return questionDto;
    }
}
