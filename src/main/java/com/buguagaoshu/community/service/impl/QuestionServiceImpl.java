package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.AdminQuestionDetailDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.dto.QuestionDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.QuestionSortType;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.QuestionContent;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionContentService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.QuestionTagService;
import com.buguagaoshu.community.service.SystemConfigService;
import com.buguagaoshu.community.service.TagClassService;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private final QuestionTagService questionTagService;

    @Autowired
    public QuestionServiceImpl(QuestionMapper questionMapper, TagClassService tagClassService,
                               ClassificationCache classificationCache, NotificationService notificationService,
                               UserBatchLoader userBatchLoader, QuestionContentService questionContentService,
                               SystemConfigService systemConfigService, QuestionTagService questionTagService) {
        this.questionMapper = questionMapper;
        this.tagClassService = tagClassService;
        this.classificationCache = classificationCache;
        this.notificationService = notificationService;
        this.userBatchLoader = userBatchLoader;
        this.questionContentService = questionContentService;
        this.systemConfigService = systemConfigService;
        this.questionTagService = questionTagService;
    }

    /** 分页结果：分页参数必须是方法内局部状态，禁止写入单例字段（并发请求会互相踩踏） */
    private record QuestionPage(List<Question> questions, long[] param, long allCount) {
    }

    /**
     * 根据传入参数，返回相对应的问题列表。
     * 条件统一由 listCondition 构建，计数与列表共用同一份条件，避免 SQL 双份维护。
     */
    private QuestionPage getQuestionList(String page, String size, String tag, Integer sort,
                                         Integer classification, Long topicId) {
        String tagName = StringUtils.trimToNull(tag);
        Integer classCode = classification != null && classification != 0 ? classification : null;
        long allQuestionCount = questionMapper.selectCountByQuery(listCondition(sort, classCode, tagName, topicId));
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.selectListByQuery(
                listQuery(sort, classCode, tagName, topicId)
                        .select(QuestionMapper.LIST_QUERY_COLUMNS)
                        .limit(param[0], param[1]));
        return new QuestionPage(questionList, param, allQuestionCount);
    }

    /** 列表过滤条件：状态 + 审核状态 + 类型 code + 话题 + 标签（仅已通过标签可命中） */
    private QueryWrapper listCondition(int sort, Integer classification, String tagName, Long topicId) {
        QueryWrapper query = QueryWrapper.create()
                .where(QUESTION.STATUS.eq(1))
                .and(QUESTION.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()))
                // 站点级列表只展示公开话题内容，私有话题帖子仅在话题页内可见
                .and(QuestionMapper.PUBLIC_TOPIC_CONDITION);
        if (classification != null) {
            query.and(QUESTION.CLASSIFICATION.eq(classification));
        }
        if (topicId != null && topicId > 0) {
            query.and(QUESTION.TOPIC_ID.eq(topicId));
        }
        if (tagName != null) {
            query.and("exists (select 1 from question_tags qt join tags t on t.id=qt.tagId " +
                    "where qt.questionId=questions.questionId and t.status=1 and t.name=?)", tagName);
        }
        if (sort == QuestionSortType.NO_COMMENT.getType()) {
            query.and(QUESTION.COMMENT_COUNT.eq(0));
        }
        return query;
    }

    /** 零回复与标签筛选按 questionId 倒序，首页最新按 alterTime 倒序（与索引匹配） */
    private QueryWrapper listQuery(int sort, Integer classification, String tagName, Long topicId) {
        boolean byId = sort == QuestionSortType.NO_COMMENT.getType() || tagName != null;
        return listCondition(sort, classification, tagName, topicId)
                .orderBy(byId ? QUESTION.QUESTION_ID.desc() : QUESTION.ALTER_TIME.desc());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createQuestion(Question question, List<QuestionContent> contents, List<String> tagNames) {
        boolean reviewRequired = systemConfigService.isReviewEnabled();
        if (question.getQuestionId() == -1) {
            question.setCreateTime(System.currentTimeMillis());
            question.setAlterTime(System.currentTimeMillis());
            question.setTag("");
            question.setReviewStatus(reviewRequired
                    ? ReviewStatusEnum.PENDING.getStatus() : ReviewStatusEnum.APPROVED.getStatus());
            int rows = questionMapper.createQuestion(question);
            questionContentService.replaceContents(question.getQuestionId(), contents);
            // 标签关联与审核状态解耦：新标签先建关联，审核通过后自动公开显示
            questionTagService.saveQuestionTags(question.getQuestionId(), tagNames, question.getUserId());
            if (reviewRequired) {
                notifyReviewSubmitted(question, true);
            } else {
                // 未开启审核：发布即可见，话题讨论数立即 +1
                tagClassService.updateTalkCount(question.getTopicId(), 1);
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
            question.setAlterTime(System.currentTimeMillis());

            boolean wasPublic = oldQuestion.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus();
            boolean willBePublic = !reviewRequired;
            long oldTopicId = oldQuestion.getTopicId() == null ? 0 : oldQuestion.getTopicId();
            long newTopicId = question.getTopicId() == null ? 0 : question.getTopicId();
            // 话题讨论数只统计公开展示的问题：进入审核先扣除，审核通过时再加回
            if (wasPublic && willBePublic && oldTopicId != newTopicId) {
                if (oldTopicId > 0) {
                    tagClassService.updateTalkCount(oldTopicId, -1);
                }
                if (newTopicId > 0) {
                    tagClassService.updateTalkCount(newTopicId, 1);
                }
            } else if (wasPublic && !willBePublic && oldTopicId > 0) {
                tagClassService.updateTalkCount(oldTopicId, -1);
            } else if (!wasPublic && willBePublic && newTopicId > 0) {
                tagClassService.updateTalkCount(newTopicId, 1);
            }

            int rows = questionMapper.updateQuestion(question);
            questionContentService.replaceContents(question.getQuestionId(), contents);
            questionTagService.saveQuestionTags(question.getQuestionId(), tagNames, question.getUserId());
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
    public PaginationDto<QuestionDto> getSomeQuestionDto(String page, String size, String tag, Integer sort,
                                                          Integer classification, Long topicId, String cursor) {
        // 游标分页：仅「最新 / 零回复」两种有序列表支持，避免深 offset 扫描
        if (cursor != null && !cursor.isBlank()
                && (sort == QuestionSortType.NO_COMMENT.getType() || sort == QuestionSortType.NEW_QUESTION.getType())) {
            return cursorPage(cursor, size, tag, sort, classification, topicId);
        }
        QuestionPage questionPage = getQuestionList(page, size, tag, sort, classification, topicId);
        List<QuestionDto> questionDtoList = toDtoList(questionPage.questions());

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
        long[] param = questionPage.param();
        if (param[2] == 0) {
            param[2] = 1;
        }
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(questionPage.allCount());
        paginationDto.setNextCursor(nextCursorOf(questionPage.questions(), sort, param[3] < param[2]));
        return paginationDto;
    }

    /** 列表 DTO 装配：批量取作者与已通过标签，避免 N+1 */
    private List<QuestionDto> toDtoList(List<Question> questionList) {
        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questionList.stream().map(Question::getUserId).toList());
        Map<Long, List<com.buguagaoshu.community.dto.QuestionTagDto>> tagMap =
                questionTagService.approvedTagsOfQuestions(
                        questionList.stream().map(Question::getQuestionId).toList());

        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setUser(userMap.get(question.getUserId()));
            questionDto.setTags(tagMap.getOrDefault(question.getQuestionId(), List.of()));
            questionDto.setTopicTitle(topicTitle(question.getTopicId()));
            questionDtoList.add(questionDto.toListItem());
        }
        return questionDtoList;
    }

    private String topicTitle(Long topicId) {
        if (topicId == null || topicId <= 0) {
            return null;
        }
        TagClass tagClass = tagClassService.findById(topicId);
        return tagClass == null ? null : tagClass.getTitle();
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

    private PaginationDto<QuestionDto> cursorPage(String cursor, String size, String tag, int sort,
                                                  Integer classification, Long topicId) {
        long n = NumberUtils.getPageAndSize("1", size, 0)[1];
        Integer classCode = classification != null && classification != 0 ? classification : null;
        String tagName = StringUtils.trimToNull(tag);
        Long topic = topicId != null && topicId > 0 ? topicId : null;

        List<Question> fetched;
        try {
            String[] parts = cursor.split(":");
            if (sort == QuestionSortType.NEW_QUESTION.getType()) {
                long alterTime = Long.parseLong(parts[0]);
                long questionId = parts.length > 1 ? Long.parseLong(parts[1]) : Long.MAX_VALUE;
                fetched = questionMapper.getQuestionsByAlterTimeCursor(alterTime, questionId, classCode, topic, tagName, n + 1, 1);
            } else {
                fetched = questionMapper.getNoCommentQuestionsByCursor(Long.parseLong(parts[0]), classCode, topic, tagName, n + 1, 1);
            }
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        boolean hasMore = fetched.size() > n;
        List<Question> questionList = hasMore ? fetched.subList(0, (int) n) : fetched;

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(toDtoList(questionList));
        paginationDto.setAllCount(countQuestions(sort, classCode, tagName, topic));
        paginationDto.setNextCursor(nextCursorOf(questionList, sort, hasMore));
        return paginationDto;
    }

    private long countQuestions(int sort, Integer classification, String tagName, Long topicId) {
        return questionMapper.selectCountByQuery(listCondition(sort, classification, tagName, topicId));
    }

    @Override
    public PaginationDto<QuestionDto> getQuestionByUserId(String page, String size, long id) {
        QueryWrapper condition = QueryWrapper.create()
                .where(QUESTION.USER_ID.eq(id))
                .and(QUESTION.STATUS.eq(1))
                .and(QUESTION.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()))
                .and(QuestionMapper.PUBLIC_TOPIC_CONDITION);
        long allQuestionCount = questionMapper.selectCountByQuery(condition);
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.selectListByQuery(
                condition.orderBy(QUESTION.QUESTION_ID.desc()).limit(param[0], param[1]));

        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questionList.stream().map(Question::getUserId).toList(), true);
        Map<Long, List<com.buguagaoshu.community.dto.QuestionTagDto>> tagMap =
                questionTagService.approvedTagsOfQuestions(
                        questionList.stream().map(Question::getQuestionId).toList());

        List<QuestionDto> questionDtoList = new ArrayList<>();
        for (Question question : questionList) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            // 个人主页的问题列表同样需要作者信息，否则前端头像与 /user/{userId} 链接会变成 undefined
            questionDto.setUser(userMap.get(question.getUserId()));
            questionDto.setTags(tagMap.getOrDefault(question.getQuestionId(), List.of()));
            questionDto.setTopicTitle(topicTitle(question.getTopicId()));
            questionDtoList.add(questionDto.toListItem());
        }
        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtoList);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allQuestionCount);
        return paginationDto;

    }

    @Override
    public PaginationDto<QuestionDto> getTopicQuestions(long topicId, String page, String size, String tag,
                                                        Integer classification) {
        String tagName = StringUtils.trimToNull(tag);
        Integer classCode = classification != null && classification != 0 ? classification : null;
        long total = questionMapper.countTopicQuestions(topicId, tagName, classCode);
        long[] param = NumberUtils.getPageAndSize(page, size, total);
        List<Question> questionList = questionMapper.selectTopicQuestions(topicId, tagName, classCode,
                param[0], param[1]);

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(toDtoList(questionList));
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(total);
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
        questionDto.setUser(user);
        questionDto.setTopicTitle(topicTitle(question.getTopicId()));
        return questionDto;
    }

    @Override
    public List<QuestionDto> selectQuestionDtosByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        List<Question> questions = questionMapper.selectListByQuery(QueryWrapper.create()
                .where(QUESTION.QUESTION_ID.in(questionIds))
                .and(QUESTION.STATUS.eq(1))
                .and(QUESTION.REVIEW_STATUS.eq(ReviewStatusEnum.APPROVED.getStatus()))
                .and(QuestionMapper.PUBLIC_TOPIC_CONDITION)
                .select(QuestionMapper.LIST_QUERY_COLUMNS));
        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questions.stream().map(Question::getUserId).toList(), true);
        List<QuestionDto> result = new ArrayList<>(questions.size());
        for (Question question : questions) {
            QuestionDto questionDto = new QuestionDto();
            BeanUtils.copyProperties(question, questionDto);
            questionDto.setUser(userMap.get(question.getUserId()));
            questionDto.setTopicTitle(topicTitle(question.getTopicId()));
            result.add(questionDto);
        }
        return result;
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
        return questionMapper.getRelevantQuestion(id, 10, 1);
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
                .and(QuestionMapper.PUBLIC_TOPIC_CONDITION)
                .select(QuestionMapper.LIST_QUERY_COLUMNS)
                .orderBy(QUESTION.ALTER_TIME.desc()).limit(page, size));
    }

    @Override
    public long getAllQuestionCount() {
        return questionMapper.selectCountByQuery(QueryWrapper.create());
    }

    @Override
    public PaginationDto<QuestionDto> getAdminQuestionList(String status, String keyword, String page, String size) {
        Integer statusFilter = parseAdminStatus(status);
        String search = StringUtil.sanitizeSearchKeyword(keyword);
        long allQuestionCount = questionMapper.selectCountByQuery(adminQuestionCondition(statusFilter, search));
        long[] param = NumberUtils.getPageAndSize(page, size, allQuestionCount);
        List<Question> questionList = questionMapper.selectListByQuery(
                adminQuestionCondition(statusFilter, search)
                        .select(QuestionMapper.LIST_QUERY_COLUMNS)
                        .orderBy(QUESTION.QUESTION_ID.desc())
                        .limit(param[0], param[1]));
        return paginationDto(questionList, allQuestionCount, param);
    }

    @Override
    public AdminQuestionDetailDto getAdminQuestionDetail(long questionId) {
        Question question = questionMapper.selectOneById(questionId);
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.QUESTION_NOT_FOUND);
        }
        AdminQuestionDetailDto detail = new AdminQuestionDetailDto();
        Map<Long, User> userMap = userBatchLoader.loadUsers(List.of(question.getUserId()), true);
        detail.setQuestion(toQuestionDto(question, userMap));
        // 管理员需要看到全部正文（含密码可见/回复可见段落）才能判断内容是否违规
        detail.setContents(questionContentService.visibleContents(questionId, question.getUserId(), null, true));
        return detail;
    }

    /** 后台筛选状态：-1 / 空 表示全部；仅允许 0 与 1 */
    private Integer parseAdminStatus(String status) {
        if (StringUtils.isBlank(status) || "-1".equals(status.trim())) {
            return null;
        }
        int value;
        try {
            value = Integer.parseInt(status.trim());
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        if (value != 0 && value != 1) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        return value;
    }

    /** 后台列表条件：删除状态 + 标题/标签关键字，计数与列表共用，避免 SQL 双份维护 */
    private QueryWrapper adminQuestionCondition(Integer status, String keyword) {
        QueryWrapper query = QueryWrapper.create();
        if (status != null) {
            query.and(QUESTION.STATUS.eq(status));
        }
        if (keyword != null) {
            query.and(QUESTION.TITLE.like(keyword).or(QUESTION.TAG.like(keyword)));
        }
        return query;
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
    @Transactional(rollbackFor = Exception.class)
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
        if (question.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()
                && question.getTopicId() != null && question.getTopicId() > 0) {
            tagClassService.updateTalkCount(question.getTopicId(), -1);
        }
        questionMapper.alterStatus(questionId, 0);
        log.info("{} {} 删除了问题 {}", isAdmin ? "管理员" : "用户", operator.getId(), questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        if (question.getReviewStatus() == ReviewStatusEnum.APPROVED.getStatus()
                && question.getTopicId() != null && question.getTopicId() > 0) {
            tagClassService.updateTalkCount(question.getTopicId(), 1);
        }
        questionMapper.alterStatus(questionId, 1);
        log.info("管理员 {} 恢复了问题 {}", admin.getId(), questionId);
    }


    public PaginationDto<QuestionDto> paginationDto(List<Question> questionList, long allQuestionCount, long[] param) {
        Map<Long, User> userMap = userBatchLoader.loadUsers(
                questionList.stream().map(Question::getUserId).toList(), true);

        List<QuestionDto> questionDtos = questionList.stream()
                .map(question -> toQuestionDto(question, userMap))
                .toList();

        PaginationDto<QuestionDto> paginationDto = new PaginationDto<>();
        paginationDto.setData(questionDtos);
        paginationDto.setPagination(param[2], param[3], param[1]);
        paginationDto.setAllCount(allQuestionCount);
        return paginationDto;
    }

    /** 问题实体 → DTO：补齐作者与所属话题标题 */
    private QuestionDto toQuestionDto(Question question, Map<Long, User> userMap) {
        QuestionDto questionDto = new QuestionDto();
        BeanUtils.copyProperties(question, questionDto);
        questionDto.setUser(userMap.get(question.getUserId()));
        questionDto.setTopicTitle(topicTitle(question.getTopicId()));
        return questionDto;
    }
}
