package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.ContentReportCreateDto;
import com.buguagaoshu.community.dto.ContentReportDetailDto;
import com.buguagaoshu.community.dto.ContentReportDto;
import com.buguagaoshu.community.dto.ContentReportHandleDto;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.User;

import java.util.Map;

/**
 * 用户举报提交与后台举报处理。
 */
public interface ContentReportService {

    /** 用户提交举报 */
    void create(ContentReportCreateDto dto, User reporter);

    /**
     * @param status  -1 全部；0 待处理；1 已受理；2 已驳回
     * @param type    question / comment，空或 all 表示全部
     * @param keyword 举报说明 / 内容快照关键字
     */
    PaginationDto<ContentReportDto> listForAdmin(String status, String type, String keyword,
                                                 String page, String size);

    ContentReportDetailDto detail(long id);

    void handle(long id, ContentReportHandleDto dto, User admin);

    /** 待处理 / 已受理 / 已驳回 / 总数 */
    Map<String, Long> statusCounts();
}
