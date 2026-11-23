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

    /** 按关联表批量原子重算使用数，避免 COUNT + 覆盖写造成并发丢更新 */
    @Update("<script>update tags t set t.usageCount = " +
            "(select count(*) from question_tags qt where qt.tagId=t.id) " +
            "where t.id in <foreach collection='tagIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int refreshUsageCounts(@Param("tagIds") java.util.Collection<Long> tagIds);

    @Select("select count(*) from question_tags where tagId=#{tagId} and questionId in " +
            "(select q.questionId from questions q join topics t on t.id=q.topicId " +
            "where q.status=1 and q.reviewStatus=1 and t.reviewStatus=1 and t.status=1 and t.visibility=0)")
    long countPublicUsage(@Param("tagId") long tagId);
}
