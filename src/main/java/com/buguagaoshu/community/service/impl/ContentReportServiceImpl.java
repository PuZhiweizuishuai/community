package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.component.UserBatchLoader;
import com.buguagaoshu.community.dto.AdminCommentDetailDto;
import com.buguagaoshu.community.dto.AdminCommentQuestionDto;
import com.buguagaoshu.community.dto.ContentReportCreateDto;
import com.buguagaoshu.community.dto.ContentReportDetailDto;
import com.buguagaoshu.community.dto.ContentReportDto;
import com.buguagaoshu.community.dto.ContentReportHandleDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.NotificationTypeEnum;
import com.buguagaoshu.community.enums.ReportReasonEnum;
import com.buguagaoshu.community.enums.ReportStatusEnum;
import com.buguagaoshu.community.enums.ReviewStatusEnum;
import com.buguagaoshu.community.enums.ReviewTargetTypeEnum;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.ContentReportMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.ContentReport;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdminCommentService;
import com.buguagaoshu.community.service.ContentReportService;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.buguagaoshu.community.model.table.ContentReportTableDef.CONTENT_REPORT;
import static com.buguagaoshu.community.model.table.QuestionTableDef.QUESTION;

/**
 * 举报处理系统实现。
 *
 * <p>举报记录保存内容快照与对象作者，处理时以当前内容状态为准：
 * 受理可选同步删除被举报内容，删除动作复用问题 / 评论管理的事务逻辑，
 * 保证评论计数、话题统计与既有通知链路一致。
 */
@Service
@Slf4j
public class ContentReportServiceImpl implements ContentReportService {

    private static final int MAX_NOTE_LENGTH = 500;
    private static final int SNAPSHOT_LENGTH = 1000;

    private final ContentReportMapper reportMapper;
    private final QuestionMapper questionMapper;
    private final CommentMapper commentMapper;
    private final UserBatchLoader userBatchLoader;
    private final NotificationService notificationService;
    private final QuestionService questionService;
    private final AdminCommentService adminCommentService;

    public ContentReportServiceImpl(ContentReportMapper reportMapper,
                                    QuestionMapper questionMapper,
                                    CommentMapper commentMapper,
                                    UserBatchLoader userBatchLoader,
                                    NotificationService notificationService,
                                    QuestionService questionService,
                                    AdminCommentService adminCommentService) {
        this.reportMapper = reportMapper;
        this.questionMapper = questionMapper;
        this.commentMapper = commentMapper;
        this.userBatchLoader = userBatchLoader;
        this.notificationService = notificationService;
        this.questionService = questionService;
        this.adminCommentService = adminCommentService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ContentReportCreateDto dto, User reporter) {
        ReviewTargetTypeEnum targetType = ReviewTargetTypeEnum.of(dto.getTargetType());
        if (targetType == null || targetType == ReviewTargetTypeEnum.TOPIC) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "举报对象类型不正确");
        }
        ReportReasonEnum reason = ReportReasonEnum.of(dto.getReason());
        if (reason == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请选择举报理由");
        }
        String description = StringUtils.trimToNull(dto.getDescription());
        if (description != null && description.length() > MAX_NOTE_LENGTH) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "补充说明不能超过 500 个字符");
        }
        if (reason == ReportReasonEnum.OTHER && description == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请补充举报说明");
        }

        long now = System.currentTimeMillis();
        ContentReport report = new ContentReport();
        report.setTargetType(targetType.getType());
        report.setTargetId(dto.getTargetId());
        report.setReporterId(reporter.getId());
        report.setReason(reason.getCode());
        report.setDescription(description);
        report.setStatus(ReportStatusEnum.PENDING.getStatus());
        report.setCreateTime(now);

        if (targetType == ReviewTargetTypeEnum.QUESTION) {
            fillQuestionTarget(report, dto.getTargetId(), reporter);
        } else {
            fillCommentTarget(report, dto.getTargetId(), reporter);
        }

        if (reportMapper.insertIfNoPending(report) != 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "你已举报过该内容，请等待管理员处理");
        }
        log.info("用户 {} 举报 {} {}，理由：{}", reporter.getId(), report.getTargetType(),
                report.getTargetId(), reason.getCode());
    }

    private void fillQuestionTarget(ContentReport report, long questionId, User reporter) {
        Question question = questionMapper.selectOneById(questionId);
        if (question == null || question.getStatus() == 0
                || question.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "举报的问题不存在或当前不可见");
        }
        if (question.getUserId() == reporter.getId()) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "不能举报自己发布的内容");
        }
        report.setQuestionId(question.getQuestionId());
        report.setTargetUserId(question.getUserId());
        report.setTargetSnapshot(truncate(question.getTitle(), SNAPSHOT_LENGTH));
    }

    private void fillCommentTarget(ContentReport report, long commentId, User reporter) {
        Comment comment = commentMapper.selectOneById(commentId);
        if (comment == null || comment.getStatus() == 0
                || comment.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "举报的评论不存在或当前不可见");
        }
        if (comment.getCommentator() == reporter.getId()) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "不能举报自己发布的内容");
        }
        Question question = questionMapper.selectOneById(comment.getQuestionId());
        if (question == null || question.getStatus() == 0
                || question.getReviewStatus() != ReviewStatusEnum.APPROVED.getStatus()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "该评论所在问题当前不可见");
        }
        report.setQuestionId(comment.getQuestionId());
        report.setTargetUserId(comment.getCommentator());
        report.setTargetSnapshot(truncate(comment.getContent(), SNAPSHOT_LENGTH));
    }

    @Override
    public PaginationDto<ContentReportDto> listForAdmin(String status, String type, String keyword,
                                                        String page, String size) {
        Integer statusFilter = parseStatus(status);
        ReviewTargetTypeEnum typeFilter = parseType(type);
        String search = StringUtil.sanitizeSearchKeyword(keyword);
        long count = reportMapper.selectCountByQuery(listQuery(statusFilter, typeFilter, search));
        long[] param = NumberUtils.getPageAndSize(page, size, count);
        List<ContentReport> reports = reportMapper.selectListByQuery(
                listQuery(statusFilter, typeFilter, search)
                        .orderBy(CONTENT_REPORT.ID.desc())
                        .limit(param[0], param[1]));

        Map<Long, Question> questionMap = loadQuestions(reports);
        Map<Long, Comment> commentMap = loadComments(reports);
        Map<Long, User> userMap = loadUsers(reports, questionMap);
        List<ContentReportDto> data = reports.stream()
                .map(report -> toDto(report, questionMap, commentMap, userMap))
                .toList();

        PaginationDto<ContentReportDto> pagination = new PaginationDto<>();
        pagination.setData(data);
        pagination.setAllCount(count);
        pagination.setPagination(param[2], param[3], param[1]);
        return pagination;
    }

    @Override
    public ContentReportDetailDto detail(long id) {
        ContentReport report = reportMapper.selectOneById(id);
        if (report == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "举报记录不存在");
        }
        List<ContentReport> reports = List.of(report);
        Map<Long, Question> questionMap = loadQuestions(reports);
        Map<Long, Comment> commentMap = loadComments(reports);
        Map<Long, User> userMap = loadUsers(reports, questionMap);

        ContentReportDetailDto detail = new ContentReportDetailDto();
        detail.setReport(toDto(report, questionMap, commentMap, userMap));

        if (ReviewTargetTypeEnum.COMMENT.getType().equals(report.getTargetType())) {
            try {
                AdminCommentDetailDto commentDetail = adminCommentService.detail(report.getTargetId());
                detail.setCommentDetail(commentDetail);
                detail.setQuestion(commentDetail.getQuestion() != null
                        ? commentDetail.getQuestion()
                        : toQuestionDto(questionMap.get(report.getQuestionId()), userMap));
            } catch (CustomizeException e) {
                // 评论可能已被彻底删除，仍返回举报快照与分析信息
                detail.setQuestion(toQuestionDto(questionMap.get(report.getQuestionId()), userMap));
            }
        } else {
            detail.setQuestion(toQuestionDto(questionMap.get(report.getQuestionId()), userMap));
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(long id, ContentReportHandleDto dto, User admin) {
        ContentReport report = reportMapper.selectOneById(id);
        if (report == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "举报记录不存在");
        }
        if (report.getStatus() != ReportStatusEnum.PENDING.getStatus()) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "该举报已被处理，请刷新后重试");
        }
        String note = StringUtils.trimToNull(dto.note());
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "处理说明不能超过 500 个字符");
        }
        boolean accepted = Boolean.TRUE.equals(dto.accepted());
        if (!accepted && note == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "驳回时必须填写处理说明");
        }

        boolean contentRemoved = false;
        if (accepted && Boolean.TRUE.equals(dto.deleteContent())) {
            contentRemoved = removeTargetContent(report, admin);
        }
        long now = System.currentTimeMillis();
        int status = accepted ? ReportStatusEnum.ACCEPTED.getStatus() : ReportStatusEnum.REJECTED.getStatus();
        if (reportMapper.handle(id, status, admin.getId(), note, now, contentRemoved ? 1 : 0) != 1) {
            throw new CustomizeException(CustomizeErrorCode.CONFLICT.getCode(), "该举报已被处理，请刷新后重试");
        }
        notifyReporter(report, admin, accepted, note, contentRemoved);
        log.info("管理员 {} 处理举报 {}：{}，内容删除：{}", admin.getId(), id,
                accepted ? "受理" : "驳回", contentRemoved);
    }

    @Override
    public Map<String, Long> statusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        long pending = reportMapper.countByStatus(ReportStatusEnum.PENDING.getStatus());
        long accepted = reportMapper.countByStatus(ReportStatusEnum.ACCEPTED.getStatus());
        long rejected = reportMapper.countByStatus(ReportStatusEnum.REJECTED.getStatus());
        counts.put("pending", pending);
        counts.put("accepted", accepted);
        counts.put("rejected", rejected);
        counts.put("total", pending + accepted + rejected);
        return counts;
    }

    private Integer parseStatus(String status) {
        if (StringUtils.isBlank(status) || "-1".equals(status.trim()) || "all".equalsIgnoreCase(status.trim())) {
            return null;
        }
        int value;
        try {
            value = Integer.parseInt(status.trim());
        } catch (NumberFormatException e) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        if (!ReportStatusEnum.isExist(value)) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        return value;
    }

    private ReviewTargetTypeEnum parseType(String type) {
        if (StringUtils.isBlank(type) || "all".equalsIgnoreCase(type.trim())) {
            return null;
        }
        ReviewTargetTypeEnum targetType = ReviewTargetTypeEnum.of(type.trim());
        if (targetType == null) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        return targetType;
    }

    private QueryWrapper listQuery(Integer status, ReviewTargetTypeEnum type, String keyword) {
        QueryWrapper query = QueryWrapper.create();
        if (status != null) {
            query.and(CONTENT_REPORT.STATUS.eq(status));
        }
        if (type != null) {
            query.and(CONTENT_REPORT.TARGET_TYPE.eq(type.getType()));
        }
        if (keyword != null) {
            query.and(CONTENT_REPORT.DESCRIPTION.like(keyword).or(CONTENT_REPORT.TARGET_SNAPSHOT.like(keyword)));
        }
        return query;
    }

    private Map<Long, Question> loadQuestions(List<ContentReport> reports) {
        Set<Long> questionIds = reports.stream()
                .map(ContentReport::getQuestionId)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionMapper.selectListByQuery(QueryWrapper.create()
                        .select(QUESTION.QUESTION_ID, QUESTION.TITLE, QUESTION.STATUS, QUESTION.REVIEW_STATUS,
                                QUESTION.USER_ID, QUESTION.CLASSIFICATION, QUESTION.TAG, QUESTION.CREATE_TIME)
                        .where(QUESTION.QUESTION_ID.in(questionIds)))
                .stream().collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
    }

    private Map<Long, Comment> loadComments(List<ContentReport> reports) {
        Set<Long> commentIds = reports.stream()
                .filter(report -> ReviewTargetTypeEnum.COMMENT.getType().equals(report.getTargetType()))
                .map(ContentReport::getTargetId)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        return commentMapper.selectListByIds(commentIds).stream()
                .collect(Collectors.toMap(Comment::getCommentId, Function.identity()));
    }

    private Map<Long, User> loadUsers(List<ContentReport> reports, Map<Long, Question> questionMap) {
        Set<Long> userIds = new HashSet<>();
        reports.forEach(report -> {
            userIds.add(report.getReporterId());
            userIds.add(report.getHandlerId());
            userIds.add(report.getTargetUserId());
        });
        questionMap.values().forEach(question -> userIds.add(question.getUserId()));
        return userBatchLoader.loadUsers(userIds);
    }

    private ContentReportDto toDto(ContentReport report, Map<Long, Question> questionMap,
                                   Map<Long, Comment> commentMap, Map<Long, User> userMap) {
        ContentReportDto dto = new ContentReportDto();
        dto.setId(report.getId());
        dto.setTargetType(report.getTargetType());
        ReviewTargetTypeEnum targetType = ReviewTargetTypeEnum.of(report.getTargetType());
        dto.setTargetTypeName(targetType == null ? "" : targetType.getName());
        dto.setTargetId(report.getTargetId());
        dto.setQuestionId(report.getQuestionId());
        dto.setTargetSnapshot(report.getTargetSnapshot());
        dto.setTargetUserId(report.getTargetUserId());
        dto.setReporterId(report.getReporterId());
        dto.setReason(report.getReason());
        dto.setReasonName(ReportReasonEnum.nameOf(report.getReason()));
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus());
        dto.setStatusName(ReportStatusEnum.nameOf(report.getStatus()));
        dto.setHandlerId(report.getHandlerId());
        dto.setHandleNote(report.getHandleNote());
        dto.setContentRemoved(report.getContentRemoved());
        dto.setCreateTime(formatTime(report.getCreateTime()));
        dto.setHandleTime(formatTime(report.getHandleTime()));

        Question question = questionMap.get(report.getQuestionId());
        if (question != null) {
            dto.setQuestionTitle(question.getTitle());
            dto.setQuestionStatus(question.getStatus());
        } else {
            dto.setQuestionTitle("问题不存在");
        }

        User targetAuthor = userMap.get(report.getTargetUserId());
        if (targetAuthor != null) {
            dto.setTargetAuthorUserId(targetAuthor.getUserId());
            dto.setTargetAuthorName(targetAuthor.getUserName());
        } else {
            dto.setTargetAuthorName("未知用户");
        }
        User reporter = userMap.get(report.getReporterId());
        if (reporter != null) {
            dto.setReporterUserId(reporter.getUserId());
            dto.setReporterName(reporter.getUserName());
        } else {
            dto.setReporterName("未知用户");
        }
        User handler = userMap.get(report.getHandlerId());
        if (handler != null) {
            dto.setHandlerName(handler.getUserName());
        }

        if (targetType == ReviewTargetTypeEnum.COMMENT) {
            Comment comment = commentMap.get(report.getTargetId());
            dto.setTargetStatus(comment == null ? 0 : comment.getStatus());
            dto.setTargetReviewStatus(comment == null ? 0 : comment.getReviewStatus());
        } else if (question != null) {
            dto.setTargetStatus(question.getStatus());
            dto.setTargetReviewStatus(question.getReviewStatus());
        }
        return dto;
    }

    private AdminCommentQuestionDto toQuestionDto(Question question, Map<Long, User> userMap) {
        if (question == null) {
            return null;
        }
        AdminCommentQuestionDto dto = new AdminCommentQuestionDto();
        dto.setQuestionId(question.getQuestionId());
        dto.setTitle(question.getTitle());
        dto.setClassification(question.getClassification());
        dto.setTag(question.getTag());
        dto.setStatus(question.getStatus());
        dto.setReviewStatus(question.getReviewStatus());
        dto.setAuthorId(question.getUserId());
        User author = userMap.get(question.getUserId());
        if (author != null) {
            dto.setAuthorUserId(author.getUserId());
            dto.setAuthorName(author.getUserName());
        }
        dto.setCreateTime(formatTime(question.getCreateTime()));
        return dto;
    }

    private boolean removeTargetContent(ContentReport report, User admin) {
        if (ReviewTargetTypeEnum.QUESTION.getType().equals(report.getTargetType())) {
            Question question = questionMapper.selectOneById(report.getTargetId());
            if (question == null || question.getStatus() == 0) {
                return false;
            }
            questionService.deleteQuestion(question.getQuestionId(), admin, true);
            return true;
        }
        Comment comment = commentMapper.selectOneById(report.getTargetId());
        if (comment == null || comment.getStatus() == 0) {
            return false;
        }
        adminCommentService.delete(comment.getCommentId(), admin);
        return true;
    }

    private void notifyReporter(ContentReport report, User admin, boolean accepted,
                                String note, boolean contentRemoved) {
        if (report.getReporterId() == admin.getId()) {
            return;
        }
        String targetName = ReviewTargetTypeEnum.QUESTION.getType().equals(report.getTargetType()) ? "问题" : "评论";
        String snapshot = truncate(report.getTargetSnapshot(), 60);
        StringBuilder content = new StringBuilder();
        if (accepted) {
            content.append("你举报的").append(targetName).append("已受理");
            if (contentRemoved) {
                content.append("，相关内容已同步删除");
            }
            content.append("。");
        } else {
            content.append("你举报的").append(targetName).append("未予受理。");
        }
        if (note != null) {
            content.append("处理说明：").append(note);
        }
        content.append(" 举报内容：").append(snapshot);
        NotificationTypeEnum type = accepted
                ? NotificationTypeEnum.REPORT_ACCEPTED : NotificationTypeEnum.REPORT_REJECTED;
        notificationService.createSystemNotification(report.getReporterId(), admin.getId(),
                report.getQuestionId(), commentIdOf(report), type.getType(), content.toString());
    }

    private long commentIdOf(ContentReport report) {
        return ReviewTargetTypeEnum.COMMENT.getType().equals(report.getTargetType()) ? report.getTargetId() : -1L;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String formatTime(long time) {
        return time > 0 ? StringUtil.foematTime(time) : "";
    }
}
