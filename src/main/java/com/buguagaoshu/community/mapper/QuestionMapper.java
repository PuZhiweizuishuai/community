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
    @Options(useGeneratedKeys = true, keyProperty = "questionId", keyColumn = "questionId")
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
     * @param status 状态
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
     * @param status 状态
     * @return 问题列表
     */
    @Select("select * from Questions where userId=#{userId} AND status=#{status} ORDER BY questionId DESC  limit #{page}, #{size}")
    List<Question> getQuestionByUserId(@Param("page") long page, @Param("size") long size, @Param("userId") long id, @Param("status") int status);

    /**
     * 带状态的计算问题数量
     * @param status 状态
     * @return 返回问题总数
     */
    @Select("SELECT COUNT(1) FROM Questions where status=#{status}")
    long getQuestionCount(@Param("status") int status);

    /**
     * 带状态的计算该用户发布的问题数量
     * @param id 用户 id
     * @param status 状态
     * @return 返回问题总数
     */
    @Select("SELECT COUNT(1) FROM Questions where userId=#{id} AND status=#{status}")
    long getUserQuestionCount(@Param("id") long id, @Param("status") int status);


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
     * 点赞数加 n
     * @param question 问题
     * @return 点赞数加 n
     * */
    @Update("update Questions set likeCount=likeCount+#{likeCount} where questionId=#{questionId}")
    int updateQuestionLikeCount(Question question);

    /**
     * 根据正则匹配相关问题（带状态，不匹配删除的问题）
     *
     * @param tag        标签
     * @param questionId 当前问题id
     * @param size       返回最大数量
     * @param status 状态
     * @return 相关问题
     */
    @Select("select * from Questions where tag regexp #{tag} and questionId!=#{questionId} and status=#{status} order by questionId desc limit #{size}")
    List<Question> getRelevantQuestion(@Param("tag") String tag, @Param("questionId") long questionId, @Param("size") int size, @Param("status") int status);


    /**
     * 根据正则实现简单的搜索功能（带状态，不搜索删除的问题）
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
     * 只统计不删除的问题
     * @param search 关键字
     * @param status 状态
     * @return  返回正则搜索的数量
     * */
    @Select("select COUNT(*) from Questions where title regexp #{search} or tag regexp #{search} and status=#{status}")
    long searchQuestionCount(@Param("search") String search, @Param("status") int status);


    /**
     * 修改帖子时间
     * @param alterTime 修改时间
     * @param questionId 问题ID
     * @return 结果
     * */
    @Update("update Questions set alterTime=#{alterTime} where questionId=#{questionId}")
    int alterQuestionAlterTime(@Param("alterTime") long alterTime, @Param("questionId") long questionId);


    /**
     * TODO 优化分页
     * 获取零回复的 问题列表 倒序
     *
     * @param page 页码
     * @param size 每页显示数量
     * @param status 状态
     * @return 问题列表
     */
    @Select("select * from Questions where status=#{status} AND commentCount=0 ORDER BY questionId DESC limit #{page}, #{size}")
    List<Question> selectQuestionUseCommentCount(@Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * @param status 状态
     * @return  返回 0 回复的问题数量
     * */
    @Select("select COUNT(*) from Questions where status=#{status} AND commentCount=0")
    long selectQuestionUseCommentCountNumber(@Param("status") int status);


    /**
     * 根据正则搜索零回复的问题
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
     * 返回正则搜索的零回复数量
     * */
    @Select("select COUNT(*) from Questions where title regexp #{search} or tag regexp #{search} and status=#{status} AND commentCount=0")
    long selectQuestionUseCommentCountBySearchNumber(@Param("search") String search, @Param("status") int status);


    /**
     * @return 所有问题数
     * */
    @Select("select COUNT(*) from Questions")
    long getAllQuestionCount();

    /**
     * TODO 优化分页
     * 获取所有问题列表，包括以及删除的 倒序
     *
     * @param page 页码
     * @param size 每页显示数量
     * @return 问题列表
     */
    @Select("select * from Questions ORDER BY questionId DESC limit #{page}, #{size}")
    List<Question> getAllQuestionList(@Param("page") long page, @Param("size") long size);


    /**
     * 根据正则搜索所有状态的问题
     *
     * @param search 搜索参数
     * @param page 页码
     * @param size 数量
     * @return 搜索结果
     * */
    @Select("select * from Questions where title regexp #{search} or tag regexp #{search} order by questionId desc limit #{page}, #{size}")
    List<Question> searchAllQuestionList(@Param("search") String search, @Param("page") long page, @Param("size") long size);


    /**
     * 包括删除的问题
     * @return  返回正则搜索的数量
     * */
    @Select("select COUNT(*) from Questions where title regexp #{search} or tag regexp #{search}")
    long searchAllQuestionListCount(@Param("search") String search);


    /**
     * 查找问题，忽略问题状态
     * @param questionId 问题id
     * @return 结果
     * */
    @Select("SELECT * FROM Questions where questionId=#{questionId}")
    Question getQuestionIgnoreStatus(long questionId);


    /**
     * 跟新问题状态
     * @param questionId 问题Id
     * @param status 状态
     * @return 结果
     * */
    @Update("update Questions set status=#{status} where questionId=#{questionId}")
    int alterStatus(@Param("questionId") long questionId, @Param("status") int status);




    /**
     *
     * 以下内容用于首页分类获取
     *
     * */


    /**
     * 查找该分类下的所有问题
     * @param classification 分类
     * @param page 页码
     * @param size 每页显示数量
     * @param status 状态
     * @return 问题列表
     * */
    @Select("select * FROM Questions where classification=#{classification} AND status=#{status} ORDER BY alterTime DESC limit #{page}, #{size}")
    List<Question> getQuestionListUseClassC(@Param("classification") String classification, @Param("page") long page, @Param("size") long size, @Param("status") int status);

    /**
     * 返回该分类下问题数量
     * */
    @Select("select COUNT(*) FROM Questions where classification=#{classification} AND status=#{status}")
    long getQuestionListUseClassCountC(@Param("classification") String classification, @Param("status") int status);


    /**
     * 查找分类（带状态，不搜索删除的问题）
     *
     * @param search 搜索参数
     * @param classification 分类
     * @param page 页码
     * @param size 数量
     * @param status 状态
     * @return 搜索结果
     * */
    @Select("select * from Questions where classification=#{classification} AND (title regexp #{search} or tag regexp #{search}) and status=#{status} order by questionId desc limit #{page}, #{size}")
    List<Question> searchQuestionC(@Param("search") String search, @Param("classification") String classification, @Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * 只统计不删除的问题
     * @param search 关键字
     * @param classification 分类
     * @param status 状态
     * @return  返回正则搜索的数量
     * */
    @Select("select COUNT(*) from Questions where classification=#{classification} AND (title regexp #{search} or tag regexp #{search}) and status=#{status}")
    long searchQuestionCountC(@Param("search") String search, @Param("classification") String classification, @Param("status") int status);


    /**
     * 查找当前分类且和标签下零回复问题
     *
     * @param search 搜索参数
     * @param page 页码
     * @param size 数量
     * @param status 状态
     * @return 搜索结果
     * */
    @Select("select * from Questions where classification=#{classification} AND (title regexp #{search} or tag regexp #{search}) and status=#{status} AND commentCount=0 order by questionId desc limit #{page}, #{size}")
    List<Question> selectQuestionUseCommentCountBySearchC(@Param("search") String search, @Param("classification") String classification, @Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * 查找当前分类且和标签下零回复问题数量
     * */
    @Select("select COUNT(*) from Questions where classification=#{classification} AND (title regexp #{search} or tag regexp #{search}) and status=#{status} AND commentCount=0")
    long selectQuestionUseCommentCountBySearchNumberC(@Param("search") String search, @Param("classification") String classification, @Param("status") int status);


    /**
     * 查找当前分类零回复的问题
     *
     * @param page 页码
     * @param size 每页显示数量
     * @param status 状态
     * @return 问题列表
     */
    @Select("select * from Questions where classification=#{classification} AND status=#{status} AND commentCount=0 ORDER BY questionId DESC limit #{page}, #{size}")
    List<Question> selectQuestionUseCommentC(@Param("classification") String classification, @Param("page") long page, @Param("size") long size, @Param("status") int status);


    /**
     * 查找当前分类零回复的问题数量
     * @param status 状态
     * @param classification 分类
     * @return  返回 0 回复的问题数量
     * */
    @Select("select COUNT(*) from Questions where classification=#{classification} AND status=#{status} AND commentCount=0")
    long selectQuestionUseCommentCountCountC(@Param("classification") String classification, @Param("status") int status);
}
