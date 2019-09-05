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
     *
     * @param question 问题
     * @return 结果
     */
    @Insert("insert into Questions(userId, title, classification, description, fileUrl, tag, createTime, alterTime) " +
            "values (#{userId}, #{title}, #{classification}, #{description}, #{fileUrl}, #{tag}, #{createTime}, #{alterTime})")
    @Options(useGeneratedKeys = true, keyProperty = "questionId")
    int createQuestion(Question question);

    /**
     * 更新问题
     */
    @Update("update Questions set title=#{title}, classification=#{classification}, description=#{description}, fileUrl=#{fileUrl}, tag=#{tag}, " +
            "alterTime=#{alterTime} where questionId=#{questionId}")
    int updateQuestion(Question question);


    /**
     * 查找问题
     *
     * @param questionId 问题id
     * @param status 状态
     * @return 问题
     */
    @Select("SELECT * FROM Questions where questionId=#{questionId} AND status=#{status}")
    Question selectQuestionById(@Param("questionId") long questionId, @Param("status") int status);

    /**
     * TODO 优化分页
     * 获取问题列表 倒序
     *
     * @param page 页码
     * @param size 每页显示数量
     * @return 问题列表
     */
    @Select("select * from Questions where status=#{status} ORDER BY alterTime DESC limit #{page}, #{size}")
    List<Question> getSomeQuestion(@Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * TODO 优化分页
     * 获取当前用户发布的问题列表 倒序
     *
     * @param page 页码
     * @param size 每页显示数量
     * @param id   用户id
     * @return 问题列表
     */
    @Select("select * from Questions where userId=#{userId} AND status=#{status} ORDER BY questionId DESC limit #{page}, #{size}")
    List<Question> getQuestionByUserId(@Param("page") long page, @Param("size") long size, @Param("userId") long id, @Param("status") int status);

    /**
     * @return 返回问题总数
     */
    @Select("SELECT COUNT(1) FROM Questions where status=#{status}")
    long getQuestionCount(@Param("status") int status);

    /**
     * @param id 用户 id
     * @return 返回问题总数
     */
    @Select("SELECT COUNT(1) FROM Questions where userId=#{id} AND status=#{status}")
    long getUserQuestionCount(long id, @Param("status") int status);


    /**
     * 阅读数加1
     *
     * @param questionId 问题 id
     * @return 阅读数加 1
     */
    @Update("update Questions set viewCount=viewCount+1 where questionId=#{questionId}")
    int updateQuestionViewCount(long questionId);

    /**
     * 评论数加 n
     *
     * @param question 问题
     * @return 评论数加 n
     */
    @Update("update Questions set commentCount=commentCount+#{commentCount} where questionId=#{questionId}")
    int updateQuestionCommentCount(Question question);

    /**
     * 根据正则匹配相关问题
     *
     * @param tag        标签
     * @param questionId 当前问题id
     * @param size       返回最大数量
     * @return 相关问题
     */
    @Select("select * from Questions where tag regexp #{tag} and questionId!=#{questionId} and status=#{status} order by questionId desc limit #{size}")
    List<Question> getRelevantQuestion(@Param("tag") String tag, @Param("questionId") long questionId, @Param("size") int size, @Param("status") int status);


    /**
     * 根据正则实现简单的搜索功能
     *
     * @param search 搜索参数
     * @param page 页码
     * @param size 数量
     * @param status 状态
     * @return 搜索结果
     * */
    @Select("select * from Questions where title regexp #{search} or tag regexp #{search} and status=#{status} order by questionId desc limit #{page}, #{size}")
    List<Question> searchQuestion(@Param("search") String search, @Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * 返回正则搜索的数量
     * */
    @Select("select COUNT(*) from Questions where title regexp #{search} or tag regexp #{search} and status=#{status}")
    long searchQuestionCount(@Param("search") String search, @Param("status") int status);


    /**
     * 修改帖子时间
     * */
    @Update("update Questions set alterTime=#{alterTime} where questionId=#{questionId}")
    int alterQuestionAlterTime(@Param("alterTime") long alterTime, @Param("questionId") long questionId);


    /**
     * TODO 优化分页
     * 获取问题列表 倒序
     *
     * @param page 页码
     * @param size 每页显示数量
     * @return 问题列表
     */
    @Select("select * from Questions where status=#{status} AND commentCount=0 ORDER BY questionId DESC limit #{page}, #{size}")
    List<Question> selectQuestionUseCommentCount(@Param("page") long page, @Param("size") long size, @Param("status") int status);

    @Select("select COUNT(*) from Questions where status=#{status} AND commentCount=0")
    long selectQuestionUseCommentCountNumber(@Param("status") int status);


    /**
     * 根据正则实现简单的搜索功能
     *
     * @param search 搜索参数
     * @param page 页码
     * @param size 数量
     * @param status 状态
     * @return 搜索结果
     * */
    @Select("select * from Questions where title regexp #{search} or tag regexp #{search} and status=#{status} AND commentCount=0 order by questionId desc limit #{page}, #{size}")
    List<Question> selectQuestionUseCommentCountBySearch(@Param("search") String search, @Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * 返回正则搜索的数量
     * */
    @Select("select COUNT(*) from Questions where title regexp #{search} or tag regexp #{search} and status=#{status} AND commentCount=0")
    long selectQuestionUseCommentCountBySearchNumber(@Param("search") String search, @Param("status") int status);
}
