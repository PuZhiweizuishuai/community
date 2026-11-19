package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.ClassificationMapper;
import com.buguagaoshu.community.model.Classification;
import com.buguagaoshu.community.service.ClassificationAdminService;
import com.buguagaoshu.community.service.SiteCacheService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 分类保存/删除可能联动更新历史问题与话题，必须整体事务；
 * 缓存失效放在事务提交后，避免回滚后缓存已被清空。
 */
@Service
public class ClassificationAdminServiceImpl implements ClassificationAdminService {

    private final ClassificationMapper mapper;
    private final ClassificationCache cache;
    private final SiteCacheService siteCache;

    public ClassificationAdminServiceImpl(ClassificationMapper mapper,
                                          ClassificationCache cache,
                                          SiteCacheService siteCache) {
        this.mapper = mapper;
        this.cache = cache;
        this.siteCache = siteCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Classification save(Classification body) {
        if (body == null || StringUtils.isBlank(body.getKind()) || StringUtils.isBlank(body.getName())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        String kind = body.getKind();
        if (!Classification.KIND_QUESTION.equals(kind) && !Classification.KIND_TOPIC.equals(kind)) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR);
        }
        String name = body.getName().trim();
        if (name.length() > 50) {
            throw new CustomizeException(400, "分类名称不能超过 50 个字符");
        }
        long now = System.currentTimeMillis();
        boolean isNew = body.getId() == null || body.getId() <= 0;
        Classification old = isNew ? null : mapper.selectById(body.getId());
        if (!isNew && old == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }

        // 编辑时若前端未回传编号，保留原编号；新增时才自动分配下一个编号
        boolean autoCode = body.getCode() == null || body.getCode() <= 0;
        int code;
        if (autoCode) {
            code = isNew ? mapper.maxCode(kind) + 1 : old.getCode();
        } else {
            code = body.getCode();
        }
        Classification sameName = mapper.selectByName(kind, name);
        Classification sameCode = mapper.selectByCode(kind, code);
        long selfId = old == null ? -1 : old.getId();
        if (sameName != null && sameName.getId() != selfId) {
            throw new CustomizeException(409, "同一类型下已存在名称为「" + name + "」的分类");
        }
        if (sameCode != null && sameCode.getId() != selfId) {
            throw new CustomizeException(409, "编号 " + code + " 已被其他分类占用");
        }

        body.setName(name);
        body.setCode(code);
        body.setKind(kind);
        body.setSortOrder(body.getSortOrder() == null ? code : body.getSortOrder());
        body.setStatus(body.getStatus() == null || body.getStatus() != 0 ? 1 : 0);
        body.setModifiedTime(now);

        if (isNew) {
            body.setCreateTime(now);
            try {
                mapper.insertSelective(body);
            } catch (DuplicateKeyException e) {
                if (!autoCode) {
                    throw new CustomizeException(409, "分类编号或名称已被占用");
                }
                // 自动编号并发撞号：重新取号重试一次
                body.setId(null);
                body.setCode(mapper.maxCode(kind) + 1);
                try {
                    mapper.insertSelective(body);
                } catch (DuplicateKeyException again) {
                    throw new CustomizeException(409, "分类编号分配冲突，请重试");
                }
            }
        } else {
            body.setCreateTime(old.getCreateTime());
            mapper.update(body);
            // 问题分类已改为按 code 存储，重命名无需同步历史问题；话题分类改编码仍需同步 topics.type
            if (Classification.KIND_TOPIC.equals(kind)
                    && old.getCode() != null && old.getCode() != code) {
                mapper.renameTopicType(old.getCode(), code);
            }
        }
        invalidateCachesAfterCommit();
        return body;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        Classification target = id <= 0 ? null : mapper.selectById(id);
        if (target == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
        }

        if (Classification.KIND_QUESTION.equals(target.getKind())) {
            long questions = mapper.countQuestionsByClassification(target.getCode());
            if (questions > 0) {
                throw new CustomizeException(409,
                        "「" + target.getName() + "」分类下还有 " + questions
                                + " 个问题，请先把这些问题移出该分类或删除后再删除分类");
            }
        } else {
            long questions = mapper.countQuestionsByTopicType(target.getCode());
            if (questions > 0) {
                throw new CustomizeException(409,
                        "「" + target.getName() + "」分类下还有 " + questions
                                + " 个问题，请先把这些问题移出该话题或删除后再删除分类");
            }
            long topics = mapper.countTopicsByType(target.getCode());
            if (topics > 0) {
                throw new CustomizeException(409,
                        "「" + target.getName() + "」分类下还有 " + topics
                                + " 个话题，请先把话题移动到其他分类或删除后再删除分类");
            }
        }

        mapper.deleteById(id);
        invalidateCachesAfterCommit();
    }

    private void invalidateCachesAfterCommit() {
        Runnable invalidate = () -> {
            cache.reload();
            siteCache.invalidate(SiteCacheService.CLASSIFICATION);
            siteCache.invalidate(SiteCacheService.TOPICS);
            siteCache.invalidate(SiteCacheService.META);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidate.run();
                }
            });
        } else {
            invalidate.run();
        }
    }
}