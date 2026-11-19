package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.dto.AdvertisementBatchDTO;
import com.buguagaoshu.community.dto.AdvertisementOperationResult;
import com.buguagaoshu.community.dto.CreateAdvertisement;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.model.Advertisement;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdvertisementService;
import com.buguagaoshu.community.util.NumberUtils;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.buguagaoshu.community.model.table.AdvertisementTableDef.ADVERTISEMENT;

/**
 * 广告 / 新闻投放服务。
 *
 * <p>相比旧实现的改进：
 * <ul>
 *   <li>投放位置统一白名单校验，天数限制 1~365，过期数据不占用名额；</li>
 *   <li>改投放位置时先从旧位置缓存移除，避免一条广告同时出现在多个位置；</li>
 *   <li>续期同一位置不重复占用名额；容量按「当前有效投放数」计算而不是 Map.size()；</li>
 *   <li>批量投放 / 批量结束在同一个 synchronized 临界区内逐条处理，容量判断不会被并发请求绕过；</li>
 *   <li>每条都返回独立回执，前端可以展示哪些成功、哪些因满位被跳过。</li>
 * </ul>
 */
@Service
public class AdvertisementServiceImpl implements AdvertisementService {

    private static final long DAY_TIME = 86_400_000L;
    private static final long MAX_DAYS = 365L;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private static final List<String> POSITIONS = List.of(
            AdvertisementCache.HOME, AdvertisementCache.PUBLISH, AdvertisementCache.QUESTION,
            AdvertisementCache.CLASS, AdvertisementCache.USER, AdvertisementCache.USER_HOME,
            AdvertisementCache.MESSAGE, AdvertisementCache.SEARCH, AdvertisementCache.NEWS);
    private static final Set<String> VALID_POSITIONS = new LinkedHashSet<>(POSITIONS);

    private final AdvertisementMapper advertisementMapper;
    private final AdvertisementCache advertisementCache;
    private final TransactionTemplate transactionTemplate;

    public AdvertisementServiceImpl(AdvertisementMapper advertisementMapper,
                                    AdvertisementCache advertisementCache,
                                    TransactionTemplate transactionTemplate) {
        this.advertisementMapper = advertisementMapper;
        this.advertisementCache = advertisementCache;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public int insertAdvertisement(CreateAdvertisement dto, User user) {
        if (!StringUtils.isNoneBlank(dto.getTitle(), dto.getUrl(), dto.getImage())) {
            return 0;
        }
        Advertisement ad = new Advertisement();
        ad.setTitle(dto.getTitle().trim());
        ad.setUrl(dto.getUrl().trim());
        ad.setImage(dto.getImage().trim());
        ad.setCreateTime(System.currentTimeMillis());
        ad.setModifiedUser(user.getId());
        ad.setPosition("");
        ad.setStartTime(0);
        ad.setEndTime(0);
        ad.setStatus(0);
        return advertisementMapper.insertSelective(ad);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int alterAdvertisement(CreateAdvertisement dto, User user) {
        if (dto.getId() == null) {
            return 0;
        }
        Advertisement ad = advertisementMapper.selectOneById(dto.getId());
        if (ad == null || !StringUtils.isNoneBlank(dto.getTitle(), dto.getUrl(), dto.getImage())) {
            return 0;
        }
        ad.setTitle(dto.getTitle().trim());
        ad.setUrl(dto.getUrl().trim());
        ad.setImage(dto.getImage().trim());
        ad.setModifiedUser(user.getId());
        int rows = advertisementMapper.alterAdvertisement(ad);
        if (rows > 0) {
            // 正在投放的广告改了内容，事务提交后再刷新缓存里的对象
            Map<Long, Object> map = mapFor(ad.getPosition());
            if (map != null && map.containsKey(ad.getId())) {
                runAfterCommit(() -> map.put(ad.getId(), ad));
            }
        }
        return rows;
    }

    @Override
    public Advertisement selectAdvertisementById(long id) {
        return advertisementMapper.selectOneById(id);
    }

    @Override
    public PaginationDto<Advertisement> getAdvertisementList(String page, String size) {
        long total = advertisementMapper.selectCountByQuery(QueryWrapper.create());
        long[] param = NumberUtils.getPageAndSize(page, size, total);
        List<Advertisement> list = advertisementMapper.selectListByQuery(QueryWrapper.create()
                .orderBy(ADVERTISEMENT.ID.desc()).limit(param[0], param[1]));
        PaginationDto<Advertisement> dto = new PaginationDto<>();
        dto.setData(list);
        if (param[2] == 0) {
            param[2] = 1;
        }
        dto.setPagination(param[2], param[3], param[1]);
        dto.setAllCount(total);
        return dto;
    }

    @Override
    public String updateAdvertisementViewCount(String id) {
        long adId;
        try {
            adId = Long.parseLong(id);
        } catch (Exception e) {
            return "";
        }
        Advertisement ad = advertisementMapper.selectOneById(adId);
        if (ad == null) {
            return "";
        }
        ad.setViewCount(1);
        advertisementMapper.updateAdvertisementViewCount(ad);
        return ad.getUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int settingAdvertisement(CreateAdvertisement dto, User user) {
        if (dto == null || dto.getId() == null) {
            return 0;
        }
        return applySetting(dto.getId(), dto.getPosition(), dto.getTime(), user).getCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeAdvertisement(CreateAdvertisement dto, User user) {
        if (dto == null || dto.getId() == null) {
            return 0;
        }
        return applyClose(dto.getId(), user).getCode();
    }

    @Override
    public synchronized List<AdvertisementOperationResult> batchSetting(AdvertisementBatchDTO batch, User user) {
        // synchronized 必须在事务外释放：编程式事务把锁包住，避免容量判断读到未提交数据
        return transactionTemplate.execute(status -> {
            List<AdvertisementOperationResult> results = new ArrayList<>();
            if (batch == null || batch.getIds() == null || batch.getIds().isEmpty()) {
                results.add(AdvertisementOperationResult.of(null, null, 0, "请至少选择一条广告"));
                return results;
            }
            if (!VALID_POSITIONS.contains(batch.getPosition())) {
                results.add(AdvertisementOperationResult.of(null, null, 0, "投放位置不合法"));
                return results;
            }
            if (batch.getTime() == null || batch.getTime() < 1 || batch.getTime() > MAX_DAYS) {
                results.add(AdvertisementOperationResult.of(null, null, 0, "投放天数需在 1 ~ 365 天之间"));
                return results;
            }
            // 去重，避免同一条广告重复提交占用名额
            Set<Long> ids = new LinkedHashSet<>(batch.getIds());
            for (Long id : ids) {
                results.add(applySetting(id, batch.getPosition(), batch.getTime(), user));
            }
            return results;
        });
    }

    @Override
    public synchronized List<AdvertisementOperationResult> batchClose(List<Long> ids, User user) {
        return transactionTemplate.execute(status -> {
            List<AdvertisementOperationResult> results = new ArrayList<>();
            if (ids == null || ids.isEmpty()) {
                results.add(AdvertisementOperationResult.of(null, null, 0, "请至少选择一条广告"));
                return results;
            }
            for (Long id : new LinkedHashSet<>(ids)) {
                results.add(applyClose(id, user));
            }
            return results;
        });
    }

    @Override
    public List<Map<String, Object>> positionStats() {
        long now = System.currentTimeMillis();
        Map<String, Long> usedByPosition = new LinkedHashMap<>();
        for (Map<String, Object> row : advertisementMapper.countActiveGroupByPosition(now)) {
            Object position = row.get("position");
            Object total = row.get("total");
            if (position instanceof String key && total instanceof Number count) {
                usedByPosition.put(key, count.longValue());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String position : POSITIONS) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("position", position);
            item.put("max", advertisementCache.AD_MAX_NUMBER);
            long used = usedByPosition.getOrDefault(position, 0L);
            item.put("used", used);
            item.put("available", Math.max(0, advertisementCache.AD_MAX_NUMBER - used));
            result.add(item);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private AdvertisementOperationResult applySetting(Long id, String position, Long days, User user) {
        if (id == null || !VALID_POSITIONS.contains(position)
                || days == null || days < 1 || days > MAX_DAYS) {
            return AdvertisementOperationResult.of(id, null, 0, "参数不合法（位置或天数）");
        }
        Advertisement ad = advertisementMapper.selectOneById(id);
        if (ad == null) {
            return AdvertisementOperationResult.of(id, null, -1, "广告不存在");
        }

        long now = System.currentTimeMillis();
        boolean renewSamePosition = position.equals(ad.getPosition())
                && ad.getStatus() == 1 && ad.getEndTime() > now;
        Map<Long, Object> target = mapFor(position);
        if (target == null) {
            return AdvertisementOperationResult.of(id, ad.getTitle(), 0, "投放位置不合法");
        }
        // 续期不占新名额；容量以数据库当前有效投放数为准，而不是本地缓存
        int used = advertisementMapper.countActiveByPosition(position, now, renewSamePosition ? id : null);
        if (!renewSamePosition && used >= advertisementCache.AD_MAX_NUMBER) {
            return AdvertisementOperationResult.of(id, ad.getTitle(), 2,
                    "「" + position + "」位置已满（上限 " + advertisementCache.AD_MAX_NUMBER + " 条）");
        }

        ad.setPosition(position);
        ad.setStatus(1);
        ad.setStartTime(now);
        ad.setEndTime(now + days * DAY_TIME);
        ad.setModifiedUser(user.getId());
        advertisementMapper.updateAdvertisementSetting(ad);
        // 事务提交后再动缓存：先从旧位置移除，再放入新位置，回滚时缓存不受影响
        runAfterCommit(() -> {
            removeFromAllMaps(id);
            target.put(ad.getId(), ad);
        });
        return AdvertisementOperationResult.of(id, ad.getTitle(), 1,
                renewSamePosition ? "续期成功" : "投放成功，到期 " + DATE_FORMATTER.format(Instant.ofEpochMilli(ad.getEndTime())));
    }

    private AdvertisementOperationResult applyClose(Long id, User user) {
        if (id == null) {
            return AdvertisementOperationResult.of(null, null, 0, "参数不合法");
        }
        Advertisement ad = advertisementMapper.selectOneById(id);
        if (ad == null) {
            return AdvertisementOperationResult.of(id, null, -1, "广告不存在");
        }
        if (ad.getStatus() != 1 && ad.getEndTime() <= 0) {
            return AdvertisementOperationResult.of(id, ad.getTitle(), 3, "该广告当前未投放");
        }
        ad.setStatus(0);
        ad.setModifiedUser(user.getId());
        ad.setStartTime(0);
        ad.setEndTime(0);
        ad.setPosition("");
        advertisementMapper.updateAdvertisementSetting(ad);
        runAfterCommit(() -> removeFromAllMaps(id));
        return AdvertisementOperationResult.of(id, ad.getTitle(), 1, "已结束投放");
    }

    /** 事务提交后再执行缓存变更；无事务上下文时立即执行 */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void removeFromAllMaps(Long id) {
        for (String position : POSITIONS) {
            Map<Long, Object> map = mapFor(position);
            if (map != null) {
                map.remove(id);
            }
        }
    }

    private Map<Long, Object> mapFor(String position) {
        if (position == null) {
            return null;
        }
        return switch (position) {
            case AdvertisementCache.HOME -> advertisementCache.getHomeAdvertisementMap();
            case AdvertisementCache.PUBLISH -> advertisementCache.getPublishAdvertisementMap();
            case AdvertisementCache.QUESTION -> advertisementCache.getQuestionAdvertisementMap();
            case AdvertisementCache.CLASS -> advertisementCache.getClassAdvertisementMap();
            case AdvertisementCache.USER -> advertisementCache.getUserAdvertisementMap();
            case AdvertisementCache.USER_HOME -> advertisementCache.getUserHomeAdvertisementMap();
            case AdvertisementCache.MESSAGE -> advertisementCache.getMessageAdvertisementMap();
            case AdvertisementCache.SEARCH -> advertisementCache.getSearchMap();
            case AdvertisementCache.NEWS -> advertisementCache.getNewsMap();
            default -> null;
        };
    }
}
