package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.QuestionTag;
import com.buguagaoshu.community.model.Tag;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

import static com.buguagaoshu.community.model.table.QuestionTagTableDef.QUESTION_TAG;

/**
 * 帖子 - 标签关联读写。展示统一 join tags 表按审核状态过滤。
 */
@Mapper
public interface QuestionTagMapper extends BaseMapper<QuestionTag> {

    default List<QuestionTag> selectByQuestionId(long questionId) {
        return selectListByQuery(QueryWrapper.create().where(QUESTION_TAG.QUESTION_ID.eq(questionId)));
    }

    default List<QuestionTag> selectByQuestionIds(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return selectListByQuery(QueryWrapper.create().where(QUESTION_TAG.QUESTION_ID.in(questionIds)));
    }

    @Delete("delete from question_tags where questionId=#{questionId}")
    int deleteByQuestionId(@Param("questionId") long questionId);

    @Delete("delete from question_tags where tagId=#{tagId}")
    int deleteByTagId(@Param("tagId") long tagId);

    @Select("select questionId from question_tags where tagId=#{tagId}")
    List<Long> selectQuestionIdsByTagId(@Param("tagId") long tagId);

    /** 冗余列刷新：该帖所有已通过标签名（按标签 id 稳定排序） */
    @Select("select t.name from tags t join question_tags qt on qt.tagId=t.id " +
            "where qt.questionId=#{questionId} and t.status=1 order by t.id")
    List<String> selectApprovedTagNames(@Param("questionId") long questionId);

    /**
     * 详情页可见标签：已通过的标签 + 待审核标签（仅帖子作者可见）。
     * 待审核标签可能由其他用户创建后被本帖复用，因此按帖子作者身份放行，而不是按标签创建人。
     */
    @Select("select t.id, t.name, t.status from tags t join question_tags qt on qt.tagId=t.id " +
            "where qt.questionId=#{questionId} and (t.status=1 or (t.status=0 and #{includePending}=1)) " +
            "order by t.status desc, t.id")
    List<Tag> selectVisibleTags(@Param("questionId") long questionId,
                                @Param("includePending") boolean includePending);

    /** 标签筛选：只返回审核通过标签关联的帖子 ID */
    @Select("select qt.questionId from question_tags qt join tags t on t.id=qt.tagId " +
            "where t.name=#{name} and t.status=1")
    List<Long> selectQuestionIdsByTagName(@Param("name") String name);
}
