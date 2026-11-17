package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.QuestionContentTypeEnum;
import com.buguagaoshu.community.enums.QuestionSortType;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.QuestionContent;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionContentService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.SystemConfigService;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.util.DateFormatUtil;
import com.buguagaoshu.community.util.NumberUtils;
import com.mybatisflex.core.query.QueryWrapper;
import com.buguagaoshu.community.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.buguagaoshu.community.model.table.QuestionTableDef.QUESTION;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:51
 */
@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {
    private final QuestionMapper questionMapper;

    private final TagClassService tagClassService;

    private final ClassificationCache classificationCache;

    private final NotificationService notificationService;

    private final UserBatchLoader userBatchLoader;

    private final QuestionContentService questionContentService;

    private final SystemConfigService systemConfigService;

    @Autowired
    public QuestionServiceImpl(QuestionMapper questionMapper, TagClassService tagClassService,
                               ClassificationCache classificationCache, NotificationService notificationService,
                               UserBatchLoader userBatchLoader, QuestionContentService questionContentService,
                               SystemConfigService systemConfigService) {
        this.questionMapper = questionMapper;
        this.tagClassService = tagClassService;
        this.classificationCache = classificationCache;
        this.notificationService = notificationService;
        this.userBatchLoader = userBatchLoader;
        this.questionContentService = questionContentService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * 分类编号 → 分类名称（动态分类表；缓存尚未加载时回退到旧枚举）。
     * Questions.classification 存的是名称，重命名分类时后台会同步更新历史数据。
     */
    private String classificationName(int code) {
        return classificationCache.questionName(code);
    }

    /** 分页结果：分页参数必须是方法内局部状态，禁止写入单例字段（并发请求会互相踩踏） */
    private record QuestionPage(List<Question> questions, long[] param, long allCount) {
    }

    /**
     * 根据传入参数，返回相对应的问题列表。
     * 条件统一由 listCondition 构建，计数与列表共用同一份条件，避免 SQL 双份维护。
     */
    private QuestionPage getQuestionList(String page, String size, String tag, Integer sort, Integer classification) {
        String keyword = StringUtil.sanitizeSearchKeyword(tag);
        if (StringUtils.isBlank(tag)) {
            keyword = null;
        }
        String className = classification != 0 ? classificationName(classification) : null;
        long allQuestionCount = questionMapper.selectCountByQuery(listCondition(sort, className, keyword));
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.selectListByQuery(
                listQuery(sort, className, keyword)
                        .select(QuestionMapper.LIST_QUERY_COLUMNS)
                        .limit(param[0], param[1]));
        return new QuestionPage(questionList, param, allQuestionCount);
    }

    /** 列表过滤条件：状态 + 审核状态 + 分类 + 标签关键字 + 零回复 */
    private QueryWrapper listCondition(int sort, String className, String keyword) {
        QueryWrapper query = QueryWrapper.create()
                .where(QUESTION.STATUS.eq(1))
                .and(QUESTION.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()));
        if (className != null) {
            query.and(QUESTION.CLASSIFICATION.eq(className));
        }
        if (keyword != null) {
            query.and(QUESTION.TITLE.like(keyword).or(QUESTION.TAG.like(keyword)));
        }
        if (sort == QuestionSortType.NO_COMMENT.getType()) {
            query.and(QUESTION.COMMENT_COUNT.eq(0));
        }
        return query;
    }

    /** 零回复与标签筛选按 questionId 倒序，首页最新按 alterTime 倒序（与索引匹配） */
    private QueryWrapper listQuery(int sort, String className, String keyword) {
        boolean byId = sort == QuestionSortType.NO_COMMENT.getType() || keyword != null;
        return listCondition(sort, className, keyword)
                .orderBy(byId ? QUESTION.QUESTION_ID.desc() : QUESTION.ALTER_TIME.desc());
    }


    @Override
    @Transactional
    public int createQuestion(Question question, List<QuestionContent> contents) {
        // description 只保留普通段落的公开副本，受保护内容不写入 Questions 表
        question.setDescription(contents.stream()
                .filter(c -> c.getType() == QuestionContentTypeEnum.NORMAL.getType())
                .map(QuestionContent::getContent)
                .collect(Collectors.joining("\n\n")));
        boolean reviewRequired = systemConfigService.isReviewEnabled();
        if (question.getQuestionId() == -1) {
            question.setCreateTime(System.currentTimeMillis());
            question.setAlterTime(System.currentTimeMillis());
            question.setReviewStatus(reviewRequired
                    ? ReviewStatusEnum.PENDING.getStatus() : ReviewStatusEnum.APPROVED.getStatus());
            int rows = questionMapper.createQuestion(question);
            questionContentService.replaceContents(question.getQuestionId(), contents);
            if (reviewRequired) {
                notifyReviewSubmitted(question, true);
            } else {
                // 未开启审核：发布即可见，话题讨论数立即 +1
                tagClassService.updateTalkCount(question.getTag(), 1);
            }
            return rows;
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

            boolean wasPublic = oldQuestion.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus();
            boolean willBePublic = !reviewRequired;
            // 话题讨论数只统计公开展示的问题：进入审核先扣除，审核通过时再加回
            if (wasPublic && willBePublic) {
                tagClassService.alterQuestionTalkCount(newTags, oldTags);
            } else if (wasPublic) {
                tagClassService.updateTalkCount(oldQuestion.getTag(), -1);
            } else if (willBePublic) {
                tagClassService.updateTalkCount(question.getTag(), 1);
            }

            int rows = questionMapper.updateQuestion(question);
            questionContentService.replaceContents(question.getQuestionId(), contents);
            question.setReviewStatus(willBePublic
                    ? ReviewStatusEnum.APPROVED.getStatus() : ReviewStatusEnum.PENDING.getStatus());
            // 修改同样需要重新审核，否则用户可以用「先发合规内容，通过后改违规内容」绕过审核
            questionMapper.resetReview(question.getQuestionId(), question.getReviewStatus());
            if (reviewRequired && oldQuestion.getReviewStatus() != ReviewStatusEnum.PENDING.getStatus()) {
                notifyReviewSubmitted(question, false);
            }
            return rows;
        }
    }

    /**
     * 问题进入审核区时给作者一条系统消息，说明为什么暂时看不到自己的问题。
     */
    private void notifyReviewSubmitted(Question question, boolean created) {
        String content = created
                ? "你的问题已提交审核，通过后将公开展示"
                : "你的问题修改已提交审核，通过后将重新公开展示";
        notificationService.createSystemNotification(question.getUserId(), question.getUserId(),
                question.getQuestionId(), -1,
                NotificationTypeEnum.REVIEW_QUESTION_PENDING.getType(),
                content + "：「" + StringUtil.subString(question.getTitle(), 40) + "」");
    }

    @Override
    public PaginationDto<QuestionDto> getSomeQuestionDto(String page, String size, String tag, Integer sort, Integer classification, String cursor) {
        // 游标分页：仅「最新 / 零回复」两种有序列表支持，避免深 offset 扫描
        if (cursor != null && !cursor.isBlank()
                && (sort == QuestionSortType.NO_COMMENT.getType() || sort == QuestionSortType.NEW_QUESTION.getType())) {
            return cursorPage(cursor, size, tag, sort, classification);
        }
        QuestionPage questionPage = getQuestionList(page, size, tag, sort, classification);
        List<Question> questionList = questionPage.questions();
        long[] param = questionPage.param();

        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questionList.stream().map(Question::getUserId).toList());

        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(DateFormatUtil.getTimeDifference(question.getCreateTime(), System.currentTimeMillis()));
            questionDto.setAlterTime(DateFormatUtil.getTimeDifference(question.getAlterTime(), System.currentTimeMillis()));
            questionDto.setUser(userMap.get(question.getUserId()));
            questionDtoList.add(questionDto.toListItem());
        }

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
        if (param[2] == 0) {
            param[2] = 1;
        }
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(questionPage.allCount());
        paginationDto.setNextCursor(nextCursorOf(questionList, sort, param[3] < param[2]));
        return paginationDto;
    }

    /** 列表末尾元素生成下一页游标；「最新」用 (alterTime, questionId) 复合游标防止同时间戳丢行 */
    private String nextCursorOf(List<Question> list, int sort, boolean hasMore) {
        if (!hasMore || list.isEmpty()) {
            return null;
        }
        Question last = list.get(list.size() - 1);
        return sort == QuestionSortType.NEW_QUESTION.getType()
                ? last.getAlterTime() + ":" + last.getQuestionId()
                : Long.toString(last.getQuestionId());
    }

    private PaginationDto<QuestionDto> cursorPage(String cursor, String size, String tag, int sort, int classification) {
        long n = NumberUtils.getPageAndSize("1", size, 0)[1];
        String className = classification != 0 ? classificationName(classification) : null;
        String keyword = StringUtils.isBlank(tag) ? null : StringUtil.sanitizeSearchKeyword(tag);

        List<Question> fetched;
        try {
            String[] parts = cursor.split(":");
            if (sort == QuestionSortType.NEW_QUESTION.getType()) {
                long alterTime = Long.parseLong(parts[0]);
                long questionId = parts.length > 1 ? Long.parseLong(parts[1]) : Long.MAX_VALUE;
                fetched = questionMapper.getQuestionsByAlterTimeCursor(alterTime, questionId, className, keyword, n + 1, 1);
            } else {
                fetched = questionMapper.getNoCommentQuestionsByCursor(Long.parseLong(parts[0]), className, keyword, n + 1, 1);
            }
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        boolean hasMore = fetched.size() > n;
        List<Question> questionList = hasMore ? fetched.subList(0, (int) n) : fetched;

        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questionList.stream().map(Question::getUserId).toList());
        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(DateFormatUtil.getTimeDifference(question.getCreateTime(), System.currentTimeMillis()));
            questionDto.setAlterTime(DateFormatUtil.getTimeDifference(question.getAlterTime(), System.currentTimeMillis()));
            questionDto.setUser(userMap.get(question.getUserId()));
            questionDtoList.add(questionDto.toListItem());
        }

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
        paginationDto.setAllCount(countQuestions(sort, className, keyword));
        paginationDto.setNextCursor(nextCursorOf(questionList, sort, hasMore));
        return paginationDto;
    }

    private long countQuestions(int sort, String className, String keyword) {
        return questionMapper.selectCountByQuery(listCondition(sort, className, keyword));
    }

    @Override
    public PaginationDto<QuestionDto> getQuestionByUserId(String page, String size, long id) {
        QueryWrapper condition = QueryWrapper.create()
                .where(QUESTION.USER_ID.eq(id))
                .and(QUESTION.STATUS.eq(1))
                .and(QUESTION.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()));
        long allQuestionCount = questionMapper.selectCountByQuery(condition);
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.selectListByQuery(
                condition.orderBy(QUESTION.QUESTION_ID.desc()).limit(param[0], param[1]));

        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questionList.stream().map(Question::getUserId).toList(), true);

        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(DateFormatUtil.getTimeDifference(question.getCreateTime(), System.currentTimeMillis()));
            questionDto.setAlterTime(DateFormatUtil.getTimeDifference(question.getAlterTime(), System.currentTimeMillis()));
            // 个人主页的问题列表同样需要作者信息，否则前端头像与 /user/{userId} 链接会变成 undefined
            questionDto.setUser(userMap.get(question.getUserId()));
            questionDtoList.add(questionDto.toListItem());
        }
        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allQuestionCount);
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

        Question question = questionMapper.selectOneById(id);
        if (question == null) {
            return null;
        }

        User user = userBatchLoader.loadUsers(List.of(question.getUserId()), true).get(question.getUserId());
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
    public List<Question> getRelevantQuestion(String questionId) {
        long id;
        try {
            id = Long.parseLong(questionId);
        } catch (Exception e) {
            return List.of();
        }
        String tag = questionMapper.selectTagById(id);
        if (tag == null || tag.isBlank()) {
            return List.of();
        }
        List<String> tags = Arrays.stream(tag.split(",|，"))
                .map(StringUtil::sanitizeSearchKeyword)
                .filter(Objects::nonNull)
                .toList();
        if (tags.isEmpty()) {
            return List.of();
        }
        return questionMapper.getRelevantQuestion(tags, id, 10, 1);
    }

    @Override
    public PaginationDto<QuestionDto> searchQuestion(String search, String page, String size) {
        String keyword = StringUtil.sanitizeFulltextKeyword(search);
        if (keyword == null) {
            return paginationDto(List.of(), 0, NumberUtils.getPageAndSize(page, size, 0));
        }
        long allQuestionCount = questionMapper.fulltextSearchQuestionCount(keyword, 1);
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.fulltextSearchQuestion(keyword, param[0], param[1], 1);
        return paginationDto(questionList, allQuestionCount, param);
    }

    @Override
    public List<Question> getQuestionListForTag(long page, long size) {
        return questionMapper.selectListByQuery(QueryWrapper.create()
                .where(QUESTION.STATUS.eq(1))
                .and(QUESTION.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()))
                .select(QuestionMapper.LIST_QUERY_COLUMNS)
                .orderBy(QUESTION.ALTER_TIME.desc()).limit(page, size));
    }

    @Override
    public long getAllQuestionCount() {
        return questionMapper.selectCountByQuery(QueryWrapper.create());
    }

    @Override
    public PaginationDto<QuestionDto> getAllQuestionList(String page, String size) {
        long allQuestionCount = questionMapper.selectCountByQuery(QueryWrapper.create());
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.selectListByQuery(QueryWrapper.create()
                .orderBy(QUESTION.QUESTION_ID.desc()).limit(param[0], param[1]));
        return paginationDto(questionList, allQuestionCount, param);
    }

    @Override
    public PaginationDto<QuestionDto> searchAllQuestionList(String search, String page, String size) {
        String keyword = StringUtil.sanitizeFulltextKeyword(search);
        if (keyword == null) {
            return paginationDto(List.of(), 0, NumberUtils.getPageAndSize(page, size, 0));
        }
        long allQuestionCount = questionMapper.fulltextSearchAllQuestionListCount(keyword);
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.fulltextSearchAllQuestionList(keyword, param[0], param[1]);
        return paginationDto(questionList, allQuestionCount, param);
    }

    @Override
    public long getUserQuestionCount(long id, int status) {
        return questionMapper.selectCountByQuery(QueryWrapper.create()
                .where(QUESTION.USER_ID.eq(id)).and(QUESTION.STATUS.eq(status)));
    }

    @Override
    public int updateFollowCount(Question question) {
        return questionMapper.updateQuestionFollowCount(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(long questionId, User operator, boolean isAdmin) {
        Question question = questionMapper.selectOneById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        if (!isAdmin && question.getUserId() != operator.getId()) {
            throw new CustomizeException(CustomizeErrorCode.FORBIDDEN);
        }
        if (question.getStatus() == 0) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_DELETED);
        }
        notificationService.notifyQuestionStatusChange(question, operator.getId(),
                NotificationTypeEnum.SYSTEM_DELETE_QUESTION, NotificationTypeEnum.SYSTEM_DELETE_COMMENT);
        // 只有公开展示过的问题才计入话题讨论数
        if (question.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
            tagClassService.updateTalkCount(question.getTag(), -1);
        }
        questionMapper.alterStatus(questionId, 0);
        log.info("{} {} 删除了问题 {}", isAdmin ? "管理员" : "用户", operator.getId(), questionId);
    }

    @Override
    @Transactional
    public void recoveryQuestion(long questionId, User admin) {
        Question question = questionMapper.selectOneById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        if (question.getStatus() == 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT);
        }
        notificationService.notifyQuestionStatusChange(question, admin.getId(),
                NotificationTypeEnum.SYSTEM_RESET_QUESTION, NotificationTypeEnum.SYSTEM_RESET_COMMENT);
        if (question.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()) {
            tagClassService.updateTalkCount(question.getTag(), 1);
        }
        questionMapper.alterStatus(questionId, 1);
        log.info("管理员 {} 恢复了问题 {}", admin.getId(), questionId);
    }


    public PaginationDto<QuestionDto> paginationDto(List<Question> questionList, long allQuestionCount, long[] param) {
        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questionList.stream().map(Question::getUserId).toList(), true);

        List<QuestionDto> questionDtos = questionList.stream().map(question -> {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setCreateTime(StringUtil.foematTime(question.getCreateTime()));
            questionDto.setAlterTime(StringUtil.foematTime(question.getAlterTime()));
            questionDto.setUser(userMap.get(question.getUserId()));
            return questionDto;
        }).toList();

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtos);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allQuestionCount);
        return paginationDto;
    }
}