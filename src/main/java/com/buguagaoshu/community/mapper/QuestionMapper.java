package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Question;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.buguagaoshu.community.model.table.QuestionTableDef.QUESTION;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-15 15:47
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /** 列表查询专用列：不含 description 正文，避免列表页传输/读取大字段 */
    String LIST_COLUMNS = "questionId, userId, title, classification, fileUrl, viewCount, " +
            "commentCount, likeCount, followCount, tag, createTime, alterTime, status";

    /** 与 LIST_COLUMNS 对应的类型安全查询列，供 QueryWrapper.select 使用 */
    QueryColumn[] LIST_QUERY_COLUMNS = {
            QUESTION.QUESTION_ID, QUESTION.USER_ID, QUESTION.TITLE, QUESTION.CLASSIFICATION, QUESTION.FILE_URL,
            QUESTION.VIEW_COUNT, QUESTION.COMMENT_COUNT, QUESTION.LIKE_COUNT, QUESTION.FOLLOW_COUNT, QUESTION.TAG,
            QUESTION.CREATE_TIME, QUESTION.ALTER_TIME, QUESTION.STATUS
    };

    /**
     * 插入问题。viewCount/status 等列依赖数据库默认值，不能按实体全字段插入，保留显式列 SQL。
     */
    @Insert("insert into Questions(userId, title, classification, description, fileUrl, tag, createTime, alterTime) " +
            "values (#{userId}, #{title}, #{classification}, #{description}, #{fileUrl}, #{tag}, #{createTime}, #{alterTime})")
    @Options(useGeneratedKeys = true, keyProperty = "questionId", keyColumn = "questionId")
    int createQuestion(Question question);

    /**
     * 更新问题内容
     */
    default int updateQuestion(Question question) {
        Question update = UpdateEntity.of(Question.class, question.getQuestionId());
        update.setTitle(question.getTitle());
        update.setClassification(question.getClassification());
        update.setDescription(question.getDescription());
        update.setFileUrl(question.getFileUrl());
        update.setTag(question.getTag());
        update.setAlterTime(question.getAlterTime());
        return update(update);
    }

    default Question selectQuestionById(long questionId, int status) {
        return selectOneByQuery(QueryWrapper.create()
                .where(QUESTION.QUESTION_ID.eq(questionId)).and(QUESTION.STATUS.eq(status)));
    }

    /**
     * 修改问题状态
     */
    default int alterStatus(long questionId, int status) {
        Question update = UpdateEntity.of(Question.class, questionId);
        update.setStatus(status);
        return update(update);
    }

    /**
     * 修改帖子时间
     */
    default int alterQuestionAlterTime(long alterTime, long questionId) {
        Question update = UpdateEntity.of(Question.class, questionId);
        update.setAlterTime(alterTime);
        return update(update);
    }

    /**
     * 阅读数加1（原子增量）
     */
    @Update("update Questions set viewCount=viewCount+1 where questionId=#{questionId}")
    int updateQuestionViewCount(long questionId);

    /**
     * 浏览数批量回写（Redis 缓冲定时落库）
     */
    @Update("update Questions set viewCount=viewCount+#{delta} where questionId=#{questionId}")
    int increaseViewCount(@Param("questionId") long questionId, @Param("delta") long delta);

    /**
     * 评论数加 n（原子增量）
     */
    @Update("update Questions set commentCount=commentCount+#{commentCount} where questionId=#{questionId}")
    int updateQuestionCommentCount(Question question);

    /**
     * 点赞数加 n（原子增量）
     */
    @Update("update Questions set likeCount=likeCount+#{likeCount} where questionId=#{questionId}")
    int updateQuestionLikeCount(Question question);

    /**
     * 关注数加 n（原子增量）
     */
    @Update("update Questions set followCount=followCount+#{followCount} where questionId=#{questionId}")
    int updateQuestionFollowCount(Question question);

    /**
     * 根据标签匹配相关问题（LIKE 匹配，标签列表由服务端拆分，避免用户输入直接进入正则）
     */
    @Select("<script>select * from Questions where " +
            "<foreach collection='tags' item='t' open='(' separator=' OR ' close=')'>tag like concat('%', #{t}, '%')</foreach>" +
            " and questionId!=#{questionId} and status=#{status} order by questionId desc limit #{size}</script>")
    List<Question> getRelevantQuestion(@Param("tags") List<String> tags, @Param("questionId") long questionId, @Param("size") int size, @Param("status") int status);

    // ------------------------------------------------------------------
    // 关键字搜索：FULLTEXT(ngram) 布尔模式，走 ft_questions_title_tag 索引
    // ------------------------------------------------------------------

    @Select("select * from Questions where MATCH(title, tag) AGAINST(#{search} IN BOOLEAN MODE) and status=#{status} order by questionId desc limit #{page}, #{size}")
    List<Question> fulltextSearchQuestion(@Param("search") String search, @Param("page") long page, @Param("size") long size, @Param("status") int status);

    @Select("select COUNT(*) from Questions where MATCH(title, tag) AGAINST(#{search} IN BOOLEAN MODE) and status=#{status}")
    long fulltextSearchQuestionCount(@Param("search") String search, @Param("status") int status);

    @Select("select * from Questions where MATCH(title, tag) AGAINST(#{search} IN BOOLEAN MODE) order by questionId desc limit #{page}, #{size}")
    List<Question> fulltextSearchAllQuestionList(@Param("search") String search, @Param("page") long page, @Param("size") long size);

    @Select("select COUNT(*) from Questions where MATCH(title, tag) AGAINST(#{search} IN BOOLEAN MODE)")
    long fulltextSearchAllQuestionListCount(@Param("search") String search);

    // ------------------------------------------------------------------
    // 游标分页：WHERE 游标条件 + LIMIT n，避免深 offset 扫描丢弃大量行
    // 「最新」按 (alterTime, questionId) 复合游标，「零回复」按 questionId 游标
    // ------------------------------------------------------------------

    @Select("<script>select " + LIST_COLUMNS + " from Questions where status=#{status} " +
            "and (alterTime &lt; #{alterTime} or (alterTime = #{alterTime} and questionId &lt; #{questionId})) " +
            "<if test='classification != null'>and classification=#{classification} </if>" +
            "<if test='tag != null'>and (title like concat('%', #{tag}, '%') or tag like concat('%', #{tag}, '%')) </if>" +
            "order by alterTime desc, questionId desc limit #{size}</script>")
    List<Question> getQuestionsByAlterTimeCursor(@Param("alterTime") long alterTime,
                                                 @Param("questionId") long questionId,
                                                 @Param("classification") String classification,
                                                 @Param("tag") String tag,
                                                 @Param("size") long size,
                                                 @Param("status") int status);

    @Select("<script>select " + LIST_COLUMNS + " from Questions where status=#{status} and commentCount=0 and questionId &lt; #{questionId} " +
            "<if test='classification != null'>and classification=#{classification} </if>" +
            "<if test='tag != null'>and (title like concat('%', #{tag}, '%') or tag like concat('%', #{tag}, '%')) </if>" +
            "order by questionId desc limit #{size}</script>")
    List<Question> getNoCommentQuestionsByCursor(@Param("questionId") long questionId,
                                                 @Param("classification") String classification,
                                                 @Param("tag") String tag,
                                                 @Param("size") long size,
                                                 @Param("status") int status);

    /**
     * 批量统计各用户的问题数（活跃用户榜单装配用）
     */
    @Select("<script>select userId, COUNT(*) as cnt from Questions where status=1 and userId in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " group by userId</script>")
    List<Map<String, Object>> countQuestionByUserIds(@Param("ids") Collection<Long> ids);
}