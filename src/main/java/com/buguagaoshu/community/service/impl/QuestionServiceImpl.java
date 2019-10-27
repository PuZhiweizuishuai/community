package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.enums.QuestionClassType;
import com.buguagaoshu.community.enums.QuestionSortType;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserPermission;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.service.UserPermissionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.DateFormatUtil;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Slf4j
public class QuestionServiceImpl implements QuestionService {
    private final QuestionMapper questionMapper;

    private final UserService userService;

    private final UserPermissionService userPermissionService;

    private final TagClassService tagClassService;

    private long[] param;

    @Autowired
    public QuestionServiceImpl(QuestionMapper questionMapper, UserService userService, UserPermissionService userPermissionService, TagClassService tagClassService) {
        this.questionMapper = questionMapper;
        this.userService = userService;
        this.userPermissionService = userPermissionService;
        this.tagClassService = tagClassService;
    }


    /**
     * 根据传入参数，返回相对应的问题列表
     * 这段写的太烂了，连我自己都想吐槽
     */
    private List<Question> getQuestionList(String page, String size, String tag, Integer sort, Integer classification) {
        long allQuestionCount;
        List<Question> questionList;
        if (StringUtils.isBlank(tag)) {
            // 无标签，零回复
            if (sort == QuestionSortType.NO_COMMENT.getType()) {
                // 有分类
                if(classification != QuestionClassType.ALL.getType()) {
                    allQuestionCount = questionMapper.selectQuestionUseCommentCountCountC(QuestionClassType.getNameStr(classification),1);
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.selectQuestionUseCommentC(QuestionClassType.getNameStr(classification), param[0], param[1], 1);
                } else {
                    allQuestionCount = questionMapper.selectQuestionUseCommentCountNumber(1);
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.selectQuestionUseCommentCount(param[0], param[1], 1);
                }
            } else {
                // 有分类
                if(classification != QuestionClassType.ALL.getType()) {
                    allQuestionCount = questionMapper.getQuestionListUseClassCountC(QuestionClassType.getNameStr(classification), 1);
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.getQuestionListUseClassC(QuestionClassType.getNameStr(classification), param[0], param[1], 1);
                } else {
                    // 正常的排序
                    allQuestionCount = questionMapper.getQuestionCount(1);
                    // 计算分页参数
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.getSomeQuestion(param[0], param[1], 1);
                }
            }
        } else {
            // 带标签
            if (sort == QuestionSortType.NO_COMMENT.getType()) {
                // 带排序
                if(classification != QuestionClassType.ALL.getType()) {
                    allQuestionCount = questionMapper.selectQuestionUseCommentCountBySearchNumberC(tag, QuestionClassType.getNameStr(classification), 1);
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.selectQuestionUseCommentCountBySearchC(tag, QuestionClassType.getNameStr(classification), param[0], param[1], 1);

                } else {
                    allQuestionCount = questionMapper.selectQuestionUseCommentCountBySearchNumber(tag, 1);
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.selectQuestionUseCommentCountBySearch(tag, param[0], param[1], 1);
                }
            } else {
                // 不带排序
                if(classification != QuestionClassType.ALL.getType()) {
                    allQuestionCount = questionMapper.searchQuestionCountC(tag, QuestionClassType.getNameStr(classification), 1);
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.searchQuestionC(tag, QuestionClassType.getNameStr(classification), param[0], param[1], 1);
                } else {
                    allQuestionCount = questionMapper.searchQuestionCount(tag, 1);
                    // 计算分页参数
                    param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
                    questionList = questionMapper.searchQuestion(tag, param[0], param[1], 1);
                }
            }
        }
        return questionList;
    }


    @Override
    @Transactional(rollbackFor = CustomizeException.class)
    public int createQuestion(Question question) {
        if (question.getQuestionId() == -1) {
            question.setCreateTime(System.currentTimeMillis());
            question.setAlterTime(System.currentTimeMillis());
            // 话题数 +1
            tagClassService.updateTalkCount(question.getTag(), 1);
            return questionMapper.createQuestion(question);
        } else {
            Question oldQuestion = questionMapper.selectQuestionById(question.getQuestionId(), 1);
            // 防止用户修改不属于自己的问题
            // 另一方面防止用户修改自己的问题时，改错问题
            if(oldQuestion == null ||oldQuestion.getUserId() != question.getUserId() || oldQuestion.getCreateTime() != question.getCreateTime()) {
                log.info("有人尝试非法修改，修改被拦截！");
                throw new CustomizeException(CustomizeErrorCode.INVALID_OPERATION);
            }
            String regex = ",|，";
            String[] newTags = question.getTag().split(regex);
            String[] oldTags = oldQuestion.getTag().split(regex);
            question.setAlterTime(System.currentTimeMillis());

            tagClassService.alterQuestionTalkCount(newTags, oldTags);
            return questionMapper.updateQuestion(question);
        }
    }

    /**
     * TODO 优化查询，考虑使用多表级联
     */
    @Override
    public PaginationDto<QuestionDto> getSomeQuestionDto(String page, String size, String tag, Integer sort, Integer classification) {

        List<Question> questionList = getQuestionList(page, size, tag, sort, classification);

        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            User user = userService.selectUserById(question.getUserId());
            // TODO 此处只需要头像地址就好，后期需要优化
            // 保护隐私
            user.clean();
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(DateFormatUtil.getTimeDifference(question.getCreateTime(), System.currentTimeMillis()));
            questionDto.setAlterTime(DateFormatUtil.getTimeDifference(question.getAlterTime(), System.currentTimeMillis()));
            questionDto.setUser(user);
            questionDtoList.add(questionDto);
        }

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
        if(param[2] == 0) {
            param[2] = 1;
        }
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
            questionDto.setCreateTime(DateFormatUtil.getTimeDifference(question.getCreateTime(), System.currentTimeMillis()));
            questionDto.setAlterTime(DateFormatUtil.getTimeDifference(question.getAlterTime(), System.currentTimeMillis()));
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

        Question question = questionMapper.getQuestionIgnoreStatus(id);
        if (question == null) {
            return null;
        }

        User user = userService.selectUserById(question.getUserId());
        user.clean();
        UserPermission userPermission = userPermissionService.selectUserPermissionById(user.getId());
        user.setPower(userPermission.getPower());
        QuestionDto questionDto = new QuestionDto();
        BeanUtils.copyProperties(question, questionDto);
        questionDto.setCreateTime(DateFormatUtil.getTimeDifference(question.getCreateTime(), System.currentTimeMillis()));
        questionDto.setAlterTime(DateFormatUtil.getTimeDifference(question.getAlterTime(), System.currentTimeMillis()));
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


        return paginationDto(questionList, allQuestionCount, param);
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
        return paginationDto(questionList, allQuestionCount, param);
    }

    @Override
    public PaginationDto<QuestionDto> searchAllQuestionList(String search, String page, String size) {
        long allQuestionCount = questionMapper.searchAllQuestionListCount(search);
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.searchAllQuestionList(search, param[0], param[1]);
        return paginationDto(questionList, allQuestionCount, param);


    }

    @Override
    public long getUserQuestionCount(long id, int status) {
        return questionMapper.getUserQuestionCount(id, status);
    }

    @Override
    public int updateFollowCount(Question question) {
        return questionMapper.updateQuestionFollowCount(question);
    }


    public PaginationDto<QuestionDto> paginationDto(List<Question> questionList, long allQuestionCount, long[] param) {
        Set<Long> usersId = questionList.stream().map(question -> question.getUserId()).collect(Collectors.toSet());
        List<User> users = new ArrayList<>();
        for (Long id : usersId) {
            User user = userService.selectUserById(id);
            UserPermission userPermission = userPermissionService.selectUserPermissionById(id);
            user.setPower(userPermission.getPower());
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
