package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Question;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    /**
     * 查找问题
     * @param questionId 问题id
     * @return 问题
     * */
    @Select("SELECT * FROM Questions where questionId=#{questionId}")
    Question selectQuestionById(@Param("questionId") long questionId);

    /**
     * TODO 优化分页
     * 获取问题列表
     * @param page 页码
     * @param size 每页显示数量
     * @return 问题列表
     * */
    @Select("select * from Questions limit #{page}, #{size}")
    List<Question> getSomeQuestion(@Param("page") int page, @Param("size") int size);


    /**
     * TODO 优化分页
     * 获取当前用户发布的问题列表
     * @param page 页码
     * @param size 每页显示数量
     * @param id 用户id
     * @return 问题列表
     * */
    @Select("select * from Questions where userId=#{userId} limit #{page}, #{size}")
    List<Question> getQuestionByUserId(@Param("page") int page, @Param("size") int size, @Param("userId") long id);

    /**
     * @return 返回问题总数
     * */
    @Select("SELECT COUNT(1) FROM Questions")
    int getQuestionCount();

    /**
     * @param id 用户 id
     * @return 返回问题总数
     * */
    @Select("SELECT COUNT(1) FROM Questions where userId=#{id}")
    int getUserQuestionCount(long id);
}
