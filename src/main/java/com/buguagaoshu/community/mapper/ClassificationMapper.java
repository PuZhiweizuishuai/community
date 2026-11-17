package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Classification;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import static com.buguagaoshu.community.model.table.ClassificationTableDef.CLASSIFICATION;
import static com.mybatisflex.core.query.QueryMethods.max;

/**
 * 动态分类数据访问。删除分类前的「是否还有问题」校验也放在这里，
 * 保证判断和删除在同一个数据库视角下完成。
 */
@Mapper
public interface ClassificationMapper extends BaseMapper<Classification> {

    default List<Classification> getAll() {
        return selectListByQuery(QueryWrapper.create()
                .orderBy(CLASSIFICATION.KIND.asc()).orderBy(CLASSIFICATION.SORT_ORDER.asc()).orderBy(CLASSIFICATION.ID.asc()));
    }

    default List<Classification> getEnabledByKind(String kind) {
        return selectListByQuery(QueryWrapper.create()
                .where(CLASSIFICATION.KIND.eq(kind)).and(CLASSIFICATION.STATUS.eq(1))
                .orderBy(CLASSIFICATION.SORT_ORDER.asc()).orderBy(CLASSIFICATION.ID.asc()));
    }

    default Classification selectById(long id) {
        return selectOneById(id);
    }

    default Classification selectByCode(String kind, int code) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CLASSIFICATION.KIND.eq(kind)).and(CLASSIFICATION.CODE.eq(code)));
    }

    default Classification selectByName(String kind, String name) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CLASSIFICATION.KIND.eq(kind)).and(CLASSIFICATION.NAME.eq(name)));
    }

    default int maxCode(String kind) {
        Object value = selectObjectByQuery(QueryWrapper.create()
                .select(max(CLASSIFICATION.CODE)).where(CLASSIFICATION.KIND.eq(kind)));
        return value == null ? 0 : ((Number) value).intValue();
    }

    default int update(Classification classification) {
        Classification update = UpdateEntity.of(Classification.class, classification.getId());
        update.setCode(classification.getCode());
        update.setName(classification.getName());
        update.setSortOrder(classification.getSortOrder());
        update.setStatus(classification.getStatus());
        update.setModifiedTime(classification.getModifiedTime());
        return update(update);
    }

    // ------------------------------------------------------------------
    // 删除前的使用情况校验（跨表统计，保留 SQL）
    // ------------------------------------------------------------------

    /** 问题分类：Questions.classification 存的是分类名称 */
    @Select("select count(*) from Questions where classification=#{name}")
    long countQuestionsByClassification(@Param("name") String name);

    /** 话题分类：统计归类到该分类下所有话题的帖子数（兼容中英文逗号分隔的标签） */
    @Select("select count(*) from Questions q where exists (" +
            "select 1 from topic t where t.type=#{code} " +
            "and find_in_set(t.title, replace(q.tag, '，', ',')) > 0)")
    long countQuestionsByTopicType(@Param("code") int code);

    @Select("select count(*) from topic where type=#{code}")
    long countTopicsByType(@Param("code") int code);

    // ------------------------------------------------------------------
    // 重命名 / 改编码时同步历史数据，避免产生孤儿数据（跨表更新，保留 SQL）
    // ------------------------------------------------------------------

    @Update("update Questions set classification=#{newName} where classification=#{oldName}")
    int renameQuestionClassification(@Param("oldName") String old, @Param("newName") String name);

    @Update("update topic set type=#{newCode} where type=#{oldCode}")
    int renameTopicType(@Param("oldCode") int old, @Param("newCode") int code);
}