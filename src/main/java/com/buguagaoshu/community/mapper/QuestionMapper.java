package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Question;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:47
 */
@Mapper
public interface QuestionMapper {
    /**
     * 插入问题
     * @param question 问题
     * @return 结果
     * */
    @Insert("insert into Questions(userId, title, classification, description, fileUrl, tag, createTime, alterTime) " +
            "values (#{userId}, #{title}, #{classification}, #{description}, #{fileUrl}, #{tag}, #{createTime}, #{alterTime})")
    @Options(useGeneratedKeys = true, keyProperty = "questionId")
    int createQuestion(Question question);
}
