package com.buguagaoshu.community.service;

import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.mapper.ClassificationMapper;
import com.buguagaoshu.community.mapper.CommentMapper;
import com.buguagaoshu.community.mapper.FileRecordMapper;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.Classification;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.buguagaoshu.community.model.table.AdvertisementTableDef.ADVERTISEMENT;
import static com.buguagaoshu.community.model.table.ClassificationTableDef.CLASSIFICATION;
import static com.buguagaoshu.community.model.table.CommentTableDef.COMMENT;
import static com.buguagaoshu.community.model.table.TagClassTableDef.TAG_CLASS;

/**
 * 后台数据概览聚合：把内容规模、待办审核、资源占用与系统开关收敛到一次请求，
 * 避免前端首屏并发多个统计接口。
 */
@Service
public class AdminOverviewService {

    private final CommentMapper commentMapper;
    private final TagClassMapper tagClassMapper;
    private final ClassificationMapper classificationMapper;
    private final FileRecordMapper fileRecordMapper;
    private final UserMapper userMapper;
    private final AdvertisementMapper advertisementMapper;
    private final ReviewService reviewService;
    private final SystemConfigService systemConfigService;
    private final SiteCacheService siteCacheService;
    private final ContentReportService contentReportService;

    public AdminOverviewService(CommentMapper commentMapper,
                                TagClassMapper tagClassMapper,
                                ClassificationMapper classificationMapper,
                                FileRecordMapper fileRecordMapper,
                                UserMapper userMapper,
                                AdvertisementMapper advertisementMapper,
                                ReviewService reviewService,
                                SystemConfigService systemConfigService,
                                SiteCacheService siteCacheService,
                                ContentReportService contentReportService) {
        this.commentMapper = commentMapper;
        this.tagClassMapper = tagClassMapper;
        this.classificationMapper = classificationMapper;
        this.fileRecordMapper = fileRecordMapper;
        this.userMapper = userMapper;
        this.advertisementMapper = advertisementMapper;
        this.reviewService = reviewService;
        this.systemConfigService = systemConfigService;
        this.siteCacheService = siteCacheService;
        this.contentReportService = contentReportService;
    }

    /** 概览指标：评论、话题、分类、文件、封禁、广告、审核、举报与系统状态 */
    public Map<String, Object> overview() {
        long now = System.currentTimeMillis();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("commentNumber", commentMapper.selectCountByQuery(QueryWrapper.create()
                .where(COMMENT.STATUS.eq(1))));
        data.put("topicNumber", tagClassMapper.selectCountByQuery(QueryWrapper.create()
                .where(TAG_CLASS.STATUS.eq(1))));
        data.put("questionClassificationNumber", classificationCount(Classification.KIND_QUESTION));
        data.put("topicClassificationNumber", classificationCount(Classification.KIND_TOPIC));
        data.put("fileNumber", fileRecordMapper.countNormalFiles());
        data.put("storageSize", fileRecordMapper.sumNormalFileSize());
        data.put("bannedUserNumber", userMapper.countActiveBanned(now));
        data.put("activeAdvertisementNumber", advertisementMapper.countActive(now));
        data.put("pendingReview", reviewService.pendingCounts());
        data.put("report", contentReportService.statusCounts());
        data.put("system", systemStatus());
        return data;
    }

    private long classificationCount(String kind) {
        return classificationMapper.selectCountByQuery(QueryWrapper.create()
                .where(CLASSIFICATION.KIND.eq(kind))
                .and(CLASSIFICATION.STATUS.eq(1)));
    }

    private Map<String, Object> systemStatus() {
        Map<String, Object> stats = siteCacheService.stats();
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("registerEnabled", systemConfigService.isRegisterEnabled());
        system.put("chatEnabled", systemConfigService.isChatEnabled());
        system.put("reviewEnabled", systemConfigService.isReviewEnabled());
        system.put("mailEnabled", systemConfigService.mailSettings().enabled());
        system.put("cacheSize", stats.get("size"));
        system.put("cacheHitRate", stats.get("hitRate"));
        return system;
    }
}
