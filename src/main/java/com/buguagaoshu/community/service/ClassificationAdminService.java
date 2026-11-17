package com.buguagaoshu.community.service;

import com.buguagaoshu.community.model.Classification;

/**
 * 后台分类维护：保存（含历史问题/话题联动改名）与删除，整体事务。
 */
public interface ClassificationAdminService {

    Classification save(Classification body);

    void delete(long id);
}