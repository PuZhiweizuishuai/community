package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<QuestionDto> getSomeQuestionDto() {
        List<Question> questionList = questionMapper.getSomeQuestion();
        List<QuestionDto> questionDtoList = new ArrayList<>();
        for(Question question : questionList) {
            User user = userService.selectUserById(question.getUserId());
            // TODO 此处只需要头像地址就好，后期需要优化
            // 保护隐私
            user.setPassword(null);
            user.setEmail(null);
            user.setCreationTime(null);
            user.setLastTime(null);
            user.setBirthday(null);
            user.setSchool(null);
            user.setSex(null);
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setUser(user);
            questionDtoList.add(questionDto);
        }
        return questionDtoList;
    }
}
