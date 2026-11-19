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
    String LIST_COLUMNS = "questionId, userId, title, classification, topicId, fileUrl, viewCount, " +
            "commentCount, likeCount, followCount, tag, isTop, isEssence, createTime, alterTime, status, reviewStatus";

    /** 与 LIST_COLUMNS 对应的类型安全查询列，供 QueryWrapper.select 使用 */
    QueryColumn[] LIST_QUERY_COLUMNS = {
            QUESTION.QUESTION_ID, QUESTION.USER_ID, QUESTION.TITLE, QUESTION.CLASSIFICATION,
            QUESTION.TOPIC_ID, QUESTION.FILE_URL,
            QUESTION.VIEW_COUNT, QUESTION.COMMENT_COUNT, QUESTION.LIKE_COUNT, QUESTION.FOLLOW_COUNT, QUESTION.TAG,
            QUESTION.IS_TOP, QUESTION.IS_ESSENCE,
            QUESTION.CREATE_TIME, QUESTION.ALTER_TIME, QUESTION.STATUS, QUESTION.REVIEW_STATUS
    };

    /** 审核列表列：不含 description 正文，但需要审核原因/审核人等字段 */
    QueryColumn[] REVIEW_QUERY_COLUMNS = {
            QUESTION.QUESTION_ID, QUESTION.USER_ID, QUESTION.TITLE, QUESTION.CLASSIFICATION,
            QUESTION.TOPIC_ID, QUESTION.FILE_URL,
            QUESTION.VIEW_COUNT, QUESTION.COMMENT_COUNT, QUESTION.LIKE_COUNT, QUESTION.FOLLOW_COUNT, QUESTION.TAG,
            QUESTION.IS_TOP, QUESTION.IS_ESSENCE,
            QUESTION.CREATE_TIME, QUESTION.ALTER_TIME, QUESTION.STATUS, QUESTION.REVIEW_STATUS,
            QUESTION.REVIEW_REASON, QUESTION.REVIEW_SUGGESTION, QUESTION.REVIEWER_ID, QUESTION.REVIEW_TIME
    };

    /**
     * 插入问题。viewCount/status 等列依赖数据库默认值，不能按实体全字段插入，保留显式列 SQL。
     * 正文存放在 question_content 表，description 列不再写入。
     */
    @Insert("insert into questions(userId, title, classification, topicId, fileUrl, tag, createTime, alterTime, reviewStatus) " +
            "values (#{userId}, #{title}, #{classification}, #{topicId}, #{fileUrl}, #{tag}, #{createTime}, #{alterTime}, #{reviewStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "questionId", keyColumn = "questionId")
    int createQuestion(Question question);

    /**
     * 更新问题内容
     */
    default int updateQuestion(Question question) {
        Question update = UpdateEntity.of(Question.class, question.getQuestionId());
        update.setTitle(question.getTitle());
        update.setClassification(question.getClassification());
        update.setTopicId(question.getTopicId());
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

    /** 冗余标签列刷新：唯一的写入入口在 QuestionTagService */
    @Update("update questions set tag=#{tag} where questionId=#{questionId}")
    int updateQuestionTag(@Param("questionId") long questionId, @Param("tag") String tag);

    /**
     * 批量重算冗余标签列：一条 UPDATE 替代逐帖 SELECT + UPDATE。
     */
    @Update("<script>update questions q set q.tag = coalesce((select group_concat(t.name order by t.id separator ',') " +
            "from question_tags qt join tags t on t.id=qt.tagId " +
            "where qt.questionId=q.questionId and t.status=1), '') " +
            "where q.questionId in <foreach collection='questionIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int refreshQuestionTagNames(@Param("questionIds") Collection<Long> questionIds);

    /** 吧务：话题内置顶 */
    @Update("update questions set isTop=#{isTop} where questionId=#{questionId}")
    int updateTop(@Param("questionId") long questionId, @Param("isTop") int isTop);

    /** 吧务：加精 */
    @Update("update questions set isEssence=#{isEssence} where questionId=#{questionId}")
    int updateEssence(@Param("questionId") long questionId, @Param("isEssence") int isEssence);

    /**
     * 审核通过：同时清空上一次的驳回原因与修改意见
     */
    @Update("update questions set reviewStatus=1, reviewerId=#{reviewerId}, reviewTime=#{reviewTime}, " +
            "reviewReason=null, reviewSuggestion=null where questionId=#{questionId} and reviewStatus=0")
    int approveReview(@Param("questionId") long questionId,
                      @Param("reviewerId") long reviewerId,
                      @Param("reviewTime") long reviewTime);

    /**
     * 审核驳回：记录原因、修改意见与审核人
     */
    @Update("update questions set reviewStatus=2, reviewReason=#{reason}, reviewSuggestion=#{suggestion}, " +
            "reviewerId=#{reviewerId}, reviewTime=#{reviewTime} where questionId=#{questionId} and reviewStatus=0")
    int rejectReview(@Param("questionId") long questionId,
                     @Param("reason") String reason,
                     @Param("suggestion") String suggestion,
                     @Param("reviewerId") long reviewerId,
                     @Param("reviewTime") long reviewTime);

    /**
     * 重新进入审核（新发布/修改/重新提交）：清空上一次审核结果
     */
    @Update("update questions set reviewStatus=#{reviewStatus}, reviewReason=null, reviewSuggestion=null, " +
            "reviewerId=null, reviewTime=null where questionId=#{questionId}")
    int resetReview(@Param("questionId") long questionId,
                    @Param("reviewStatus") int reviewStatus);

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
    @Update("update questions set viewCount=viewCount+1 where questionId=#{questionId}")
    int updateQuestionViewCount(long questionId);

    /**
     * 浏览数批量回写（Redis 缓冲定时落库）
     */
    @Update("update questions set viewCount=viewCount+#{delta} where questionId=#{questionId}")
    int increaseViewCount(@Param("questionId") long questionId, @Param("delta") long delta);

    /**
     * 浏览数批量回写：一条 CASE WHEN 语句覆盖整批问题，替代逐行 UPDATE。
     */
    @Update("<script>update questions set viewCount = viewCount + case questionId " +
            "<foreach collection='deltas' index='id' item='delta'>when #{id} then #{delta} </foreach>" +
            "end where questionId in " +
            "<foreach collection='deltas' index='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int updateViewCountBatch(@Param("deltas") Map<Long, Long> deltas);

    /**
     * 评论数加 n（原子增量）
     */
    @Update("update questions set commentCount=commentCount+#{commentCount} where questionId=#{questionId}")
    int updateQuestionCommentCount(Question question);

    /**
     * 点赞数加 n（原子增量）
     */
    @Update("update questions set likeCount=likeCount+#{likeCount} where questionId=#{questionId}")
    int updateQuestionLikeCount(Question question);

    /**
     * 关注数加 n（原子增量）
     */
    @Update("update questions set followCount=followCount+#{followCount} where questionId=#{questionId}")
    int updateQuestionFollowCount(Question question);

    /**
     * 相关问题：与当前问题共享任意一个「已通过」标签。
     */
    @Select("select " + LIST_COLUMNS + " from questions q where q.questionId!=#{questionId} and q.status=#{status} and q.reviewStatus=1 " +
            "and exists (select 1 from question_tags qt join question_tags mine on mine.tagId=qt.tagId " +
            "join tags t on t.id=qt.tagId and t.status=1 where qt.questionId=q.questionId and mine.questionId=#{questionId}) " +
            "order by q.questionId desc limit #{size}")
    List<Question> getRelevantQuestion(@Param("questionId") long questionId, @Param("size") int size,
                                       @Param("status") int status);

    // ------------------------------------------------------------------
    // 关键字搜索：FULLTEXT(ngram) 布尔模式，走 ft_questions_title_tag 索引
    // ------------------------------------------------------------------

    @Select("select " + LIST_COLUMNS + " from questions where MATCH(title, tag) AGAINST(#{search} IN BOOLEAN MODE) and status=#{status} and reviewStatus=1 order by questionId desc limit #{page}, #{size}")
    List<Question> fulltextSearchQuestion(@Param("search") String search, @Param("page") long page, @Param("size") long size, @Param("status") int status);

    @Select("select COUNT(*) from questions where MATCH(title, tag) AGAINST(#{search} IN BOOLEAN MODE) and status=#{status} and reviewStatus=1")
    long fulltextSearchQuestionCount(@Param("search") String search, @Param("status") int status);

    // ------------------------------------------------------------------
    // 游标分页：WHERE 游标条件 + LIMIT n，避免深 offset 扫描丢弃大量行
    // 「最新」按 (alterTime, questionId) 复合游标，「零回复」按 questionId 游标
    // 标签筛选走 question_tags join，只统计审核通过的标签
    // ------------------------------------------------------------------

    @Select("<script>select " + LIST_COLUMNS + " from questions where status=#{status} and reviewStatus=1 " +
            "and (alterTime &lt; #{alterTime} or (alterTime = #{alterTime} and questionId &lt; #{questionId})) " +
            "<if test='classification != null'>and classification=#{classification} </if>" +
            "<if test='topicId != null'>and topicId=#{topicId} </if>" +
            "<if test='tag != null'>and exists (select 1 from question_tags qt join tags t on t.id=qt.tagId " +
            "where qt.questionId=questions.questionId and t.status=1 and t.name=#{tag}) </if>" +
            "order by alterTime desc, questionId desc limit #{size}</script>")
    List<Question> getQuestionsByAlterTimeCursor(@Param("alterTime") long alterTime,
                                                 @Param("questionId") long questionId,
                                                 @Param("classification") Integer classification,
                                                 @Param("topicId") Long topicId,
                                                 @Param("tag") String tag,
                                                 @Param("size") long size,
                                                 @Param("status") int status);

    @Select("<script>select " + LIST_COLUMNS + " from questions where status=#{status} and reviewStatus=1 and commentCount=0 and questionId &lt; #{questionId} " +
            "<if test='classification != null'>and classification=#{classification} </if>" +
            "<if test='topicId != null'>and topicId=#{topicId} </if>" +
            "<if test='tag != null'>and exists (select 1 from question_tags qt join tags t on t.id=qt.tagId " +
            "where qt.questionId=questions.questionId and t.status=1 and t.name=#{tag}) </if>" +
            "order by questionId desc limit #{size}</script>")
    List<Question> getNoCommentQuestionsByCursor(@Param("questionId") long questionId,
                                                 @Param("classification") Integer classification,
                                                 @Param("topicId") Long topicId,
                                                 @Param("tag") String tag,
                                                 @Param("size") long size,
                                                 @Param("status") int status);

    /**
     * 话题帖子流：置顶优先，其余按最后活跃时间倒序。
     */
    @Select("<script>select " + LIST_COLUMNS + " from questions where topicId=#{topicId} and status=1 and reviewStatus=1 " +
            "<if test='classification != null'>and classification=#{classification} </if>" +
            "<if test='tag != null'>and exists (select 1 from question_tags qt join tags t on t.id=qt.tagId " +
            "where qt.questionId=questions.questionId and t.status=1 and t.name=#{tag}) </if>" +
            "order by isTop desc, alterTime desc, questionId desc limit #{offset}, #{size}</script>")
    List<Question> selectTopicQuestions(@Param("topicId") long topicId, @Param("tag") String tag,
                                        @Param("classification") Integer classification,
                                        @Param("offset") long offset, @Param("size") long size);

    @Select("<script>select count(*) from questions where topicId=#{topicId} and status=1 and reviewStatus=1 " +
            "<if test='classification != null'>and classification=#{classification} </if>" +
            "<if test='tag != null'>and exists (select 1 from question_tags qt join tags t on t.id=qt.tagId " +
            "where qt.questionId=questions.questionId and t.status=1 and t.name=#{tag}) </if>" +
            "</script>")
    long countTopicQuestions(@Param("topicId") long topicId, @Param("tag") String tag,
                             @Param("classification") Integer classification);

    /**
     * 批量统计各用户的问题数（活跃用户榜单装配用）
     */
    @Select("<script>select userId, COUNT(*) as cnt from questions where status=1 and reviewStatus=1 and userId in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " group by userId</script>")
    List<Map<String, Object>> countQuestionByUserIds(@Param("ids") Collection<Long> ids);
}
