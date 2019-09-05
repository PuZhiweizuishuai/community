package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:51
 */
@Service
public class QuestionServiceImpl implements QuestionService {
    private final QuestionMapper questionMapper;

    private final UserService userService;

    private long[] param;

    @Autowired
    public QuestionServiceImpl(QuestionMapper questionMapper, UserService userService) {
        this.questionMapper = questionMapper;
        this.userService = userService;
    }


    private List<Question> getQuestionList(String page, String size, String tag, String sort) {
        long allQuestionCount;
        List<Question> questionList;
        if (tag == null || tag.equals("")) {
            // 零回复的帖子
            if (sort != null && sort.equals("no")) {
                allQuestionCount = questionMapper.selectQuestionUseCommentCountNumber(1);
                param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                questionList = questionMapper.selectQuestionUseCommentCount(param[0], param[1], 1);
            } else {
                // 正常的排序
                allQuestionCount = questionMapper.getQuestionCount(1);
                // 计算分页参数
                param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                questionList = questionMapper.getSomeQuestion(param[0], param[1], 1);
            }
        } else {
            // 带标签
            if (sort != null && sort.equals("no")) {
                // 带排序
                allQuestionCount = questionMapper.selectQuestionUseCommentCountBySearchNumber(tag, 1);
                param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                questionList = questionMapper.selectQuestionUseCommentCountBySearch(tag, param[0], param[1], 1);

            } else {
                // 不带排序
                allQuestionCount = questionMapper.searchQuestionCount(tag, 1);
                // 计算分页参数
                param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                questionList = questionMapper.searchQuestion(tag, param[0], param[1], 1);
            }
        }
        return questionList;
    }


    @Override
    public int createQuestion(Question question) {
        if (question.getQuestionId() == -1) {
            question.setCreateTime(System.currentTimeMillis());
            question.setAlterTime(System.currentTimeMillis());
            return questionMapper.createQuestion(question);
        } else {
            question.setAlterTime(System.currentTimeMillis());
            return questionMapper.updateQuestion(question);
        }
    }

    /**
     * TODO 优化查询，考虑使用多表级联
     */
    @Override
    public PaginationDto<QuestionDto> getSomeQuestionDto(String page, String size, String tag, String sort) {
        List<Question> questionList = getQuestionList(page, size, tag, sort);

        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            User user = userService.selectUserById(question.getUserId());
            // TODO 此处只需要头像地址就好，后期需要优化
            // 保护隐私
            user.clean();
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(StringUtil.foematTime(question.getCreateTime()));
            questionDto.setAlterTime(StringUtil.foematTime(question.getAlterTime()));
            questionDto.setUser(user);
            questionDtoList.add(questionDto);
        }

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;
    }

    @Override
    public PaginationDto<QuestionDto> getQuestionByUserId(String page, String size, long id) {
        long allQuestionCount = questionMapper.getUserQuestionCount(id, 1);
        param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.getQuestionByUserId(param[0], param[1], id, 1);
        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(StringUtil.foematTime(question.getCreateTime()));
            questionDto.setAlterTime(StringUtil.foematTime(question.getAlterTime()));
            questionDtoList.add(questionDto);
        }
        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
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
        if (id == -1) {
            return null;
        }

        Question question = questionMapper.selectQuestionById(id, 1);
        if (question == null) {
            return null;
        }

        User user = userService.selectUserById(question.getUserId());
        user.clean();
        QuestionDto questionDto = new QuestionDto();
        BeanUtils.copyProperties(question, questionDto);
        questionDto.setCreateTime(StringUtil.foematTime(question.getCreateTime()));
        questionDto.setAlterTime(StringUtil.foematTime(question.getAlterTime()));
        questionDto.setUser(user);
        return questionDto;
    }

    @Override
    public Question selectQuestionNotDtoById(String questionId) {
        long id;
        try {
            id = Long.valueOf(questionId);
        } catch (Exception e) {
            id = -1;
        }
        if (id == -1) {
            return null;
        }
        return questionMapper.selectQuestionById(id, 1);
    }

    @Override
    public int updateQuestionViewCount(long questionId) {
        return questionMapper.updateQuestionViewCount(questionId);
    }

    @Override
    public int updateQuestionCommentCount(Question question) {
        return questionMapper.updateQuestionCommentCount(question);
    }

    @Override
    public List<Question> getRelevantQuestion(QuestionDto questionDto) {
        if (questionDto.getTag() == null || questionDto.getTag().equals("")) {
            return null;
        }
        List<Question> questionDtoList = questionMapper.getRelevantQuestion(
                StringUtil.sqlSelectRegexpForRelevantQuestion(questionDto.getTag()), questionDto.getQuestionId(), 10, 1);
        return questionDtoList;
    }

    @Override
    public PaginationDto<QuestionDto> searchQuestion(String search, String page, String size) {
        long allQuestionCount = questionMapper.searchQuestionCount(search, 1);
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.searchQuestion(search, param[0], param[1], 1);


        Set<Long> usersId = questionList.stream().map(question -> question.getUserId()).collect(Collectors.toSet());

        List<User> users = new ArrayList<>();
        for (Long id : usersId) {
            User user = userService.selectUserById(id);
            user.clean();
            users.add(user);
        }
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(user -> user.getId(), user -> user));

        List<QuestionDto> questionDtos = questionList.stream().map(question -> {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(StringUtil.foematTime(question.getCreateTime()));
            questionDto.setAlterTime(StringUtil.foematTime(question.getAlterTime()));
            questionDto.setUser(userMap.get(question.getUserId()));
            return questionDto;
        }).collect(Collectors.toList());

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtos);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allQuestionCount);
        return paginationDto;
    }

    @Override
    public List<Question> getQuestionListForTag(long page, long size) {
        return questionMapper.getSomeQuestion(page, size, 1);
    }

    @Override
    public long getAllQuestionCount() {
        return questionMapper.getAllQuestionCount();
    }

    @Override
    public PaginationDto<QuestionDto> getAllQuestionList(String page, String size) {
        long allQuestionCount = questionMapper.getAllQuestionCount();
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.getAllQuestionList(param[0], param[1]);
        Set<Long> usersId = questionList.stream().map(question -> question.getUserId()).collect(Collectors.toSet());

        List<User> users = new ArrayList<>();
        for (Long id : usersId) {
            User user = userService.selectUserById(id);
            user.clean();
            users.add(user);
        }
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(user -> user.getId(), user -> user));

        List<QuestionDto> questionDtos = questionList.stream().map(question -> {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(StringUtil.foematTime(question.getCreateTime()));
            questionDto.setAlterTime(StringUtil.foematTime(question.getAlterTime()));
            questionDto.setUser(userMap.get(question.getUserId()));
            return questionDto;
        }).collect(Collectors.toList());

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtos);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allQuestionCount);
        return paginationDto;
    }
}
