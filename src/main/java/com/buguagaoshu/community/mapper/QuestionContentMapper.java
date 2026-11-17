package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.QuestionContent;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.buguagaoshu.community.model.table.QuestionContentTableDef.QUESTION_CONTENT;

@Mapper
public interface QuestionContentMapper extends BaseMapper<QuestionContent> {

    default List<QuestionContent> selectByQuestionId(long questionId) {
        return selectListByQuery(QueryWrapper.create()
                .where(QUESTION_CONTENT.QUESTION_ID.eq(questionId))
                .orderBy(QUESTION_CONTENT.SORT.asc()));
    }

    default int deleteByQuestionId(long questionId) {
        return deleteByQuery(QueryWrapper.create()
                .where(QUESTION_CONTENT.QUESTION_ID.eq(questionId)));
    }
}