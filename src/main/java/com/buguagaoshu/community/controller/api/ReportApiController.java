package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.annotation.RequirePermission;
import com.buguagaoshu.community.constant.PermissionCodes;
import com.buguagaoshu.community.dto.ContentReportCreateDto;
import com.buguagaoshu.community.dto.QuestionAccessDto;
import com.buguagaoshu.community.dto.ResultDTO;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.model.Comment;
import com.buguagaoshu.community.model.Question;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.ContentReportService;
import com.buguagaoshu.community.service.QuestionAccessService;
import com.buguagaoshu.community.util.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧举报入口：登录用户可举报问题与评论。
 */
@RestController
public class ReportApiController {

    private final ContentReportService contentReportService;
    private final QuestionMapper questionMapper;
    private final CommentMapper commentMapper;
    private final QuestionAccessService questionAccessService;

    public ReportApiController(ContentReportService contentReportService,
                               QuestionMapper questionMapper,
                               CommentMapper commentMapper,
                               QuestionAccessService questionAccessService) {
        this.contentReportService = contentReportService;
        this.questionMapper = questionMapper;
        this.commentMapper = commentMapper;
        this.questionAccessService = questionAccessService;
    }

    @PostMapping("/api/reports")
    @RequirePermission(PermissionCodes.CONTENT_REPORT_CREATE)
    public ResultDTO<Object> create(@Valid @RequestBody ContentReportCreateDto body,
                                    HttpServletRequest request) {
        User reporter = AuthUtil.requireUser(request);
        requireTargetAccess(body, reporter);
        contentReportService.create(body, reporter);
        return ResultDTO.okOf();
    }

    /** 举报前校验被举报内容当前可访问，避免枚举 ID 获取私有话题内容 */
    private void requireTargetAccess(ContentReportCreateDto body, User reporter) {
        Question question;
        if ("question".equals(body.getTargetType())) {
            question = questionMapper.selectOneById(body.getTargetId());
        } else {
            Comment comment = commentMapper.selectOneById(body.getTargetId());
            question = comment == null ? null : questionMapper.selectOneById(comment.getQuestionId());
        }
        if (question == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        QuestionAccessDto access = questionAccessService.resolve(question, reporter, null);
        if (!access.visible()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
        if (!access.canViewContent()) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }
    }
}
