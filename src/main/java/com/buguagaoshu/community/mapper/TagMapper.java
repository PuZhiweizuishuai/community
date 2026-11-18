package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Tag;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import static com.buguagaoshu.community.model.table.TagTableDef.TAG;

/**
 * 标签数据访问。名称唯一键由数据库兜底，并发重复创建时捕获 DuplicateKeyException 后复用。
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    default Tag selectByName(String name) {
        return selectOneByQuery(QueryWrapper.create().where(TAG.NAME.eq(name)));
    }

    /** 热门标签/发布页建议：只取审核通过的标签 */
    default List<Tag> selectApproved(long offset, long size) {
        return selectListByQuery(QueryWrapper.create()
                .where(TAG.STATUS.eq(1))
                .orderBy(TAG.USAGE_COUNT.desc(), TAG.ID.asc())
                .limit(offset, size));
    }

    default int updateStatus(long id, int status, long reviewerId, long reviewTime, String rejectReason) {
        Tag update = UpdateEntity.of(Tag.class, id);
        update.setStatus(status);
        update.setReviewerId(reviewerId);
        update.setReviewTime(reviewTime);
        update.setRejectReason(rejectReason);
        return update(update);
    }

    default int updateName(long id, String name) {
        Tag update = UpdateEntity.of(Tag.class, id);
        update.setName(name);
        return update(update);
    }

    @Update("update tags set usageCount=#{usageCount} where id=#{id}")
    int updateUsageCount(@Param("id") long id, @Param("usageCount") long usageCount);

    @Select("select count(*) from question_tags where tagId=#{tagId}")
    long countUsage(@Param("tagId") long tagId);

    @Select("select count(*) from question_tags where tagId=#{tagId} and questionId in " +
            "(select questionId from questions where status=1 and reviewStatus=1)")
    long countPublicUsage(@Param("tagId") long tagId);
}
