package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TagClass;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import static com.buguagaoshu.community.model.table.TagClassTableDef.TAG_CLASS;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:29
 */
@Mapper
public interface TagClassMapper extends BaseMapper<TagClass> {

    /**
     * 更新讨论数（原子增量）
     */
    @Update("update topics set talkCount=talkCount+#{talkCount} where id=#{id}")
    int updateTagTalkCount(TagClass tagClass);

    /**
     * 更新关注数（原子增量）
     */
    @Update("update topics set followCount=followCount+#{followCount} where id=#{id}")
    int updateTagFollowCount(TagClass tagClass);

    /**
     * 编辑话题信息；标题、图标、简介、分类、状态可改，统计值保持不变。
     */
    default int updateTagClass(TagClass tagClass) {
        TagClass update = UpdateEntity.of(TagClass.class, tagClass.getId());
        update.setTitle(tagClass.getTitle());
        update.setImage(tagClass.getImage());
        update.setSimpleDesc(tagClass.getSimpleDesc());
        update.setType(tagClass.getType());
        update.setStatus(tagClass.getStatus());
        update.setVisibility(tagClass.getVisibility());
        update.setModifiedTime(tagClass.getModifiedTime());
        return update(update);
    }

    /** 后台话题列表：过滤与排序下推到数据库，避免全表加载后内存筛选 */
    default List<TagClass> selectAdminList(Integer type, Integer reviewStatus, Integer visibility, String keyword) {
        QueryWrapper query = QueryWrapper.create();
        if (type != null) {
            query.and(TAG_CLASS.TYPE.eq(type));
        }
        if (reviewStatus != null) {
            query.and(TAG_CLASS.REVIEW_STATUS.eq(reviewStatus));
        }
        if (visibility != null) {
            query.and(TAG_CLASS.VISIBILITY.eq(visibility));
        }
        if (keyword != null) {
            query.and(TAG_CLASS.TITLE.like(keyword).or(TAG_CLASS.SIMPLE_DESC.like(keyword)));
        }
        query.orderBy(TAG_CLASS.TYPE.asc()).orderBy(TAG_CLASS.ID.asc());
        return selectListByQuery(query);
    }

    /** 我创建的话题 */
    default List<TagClass> selectByCreator(long userId) {
        return selectListByQuery(QueryWrapper.create()
                .where(TAG_CLASS.CREATED_BY.eq(userId))
                .orderBy(TAG_CLASS.ID.desc()));
    }

    /** 吧主修改简介 / 图标 / 可见性 */
    default int updateSettings(long id, String simpleDesc, String image, int visibility, long modifiedTime) {
        TagClass update = UpdateEntity.of(TagClass.class, id);
        update.setSimpleDesc(simpleDesc);
        update.setImage(image);
        update.setVisibility(visibility);
        update.setModifiedTime(modifiedTime);
        return update(update);
    }

    /** 管理员审核待审话题：CAS 防止重复处理 */
    @Update("update topics set reviewStatus=#{reviewStatus}, status=#{status}, " +
            "reviewReason=#{reason}, reviewerId=#{reviewerId}, reviewTime=#{reviewTime}, " +
            "modifiedTime=#{reviewTime} where id=#{id} and reviewStatus=0")
    int reviewPending(@Param("id") long id,
                      @Param("reviewStatus") int reviewStatus,
                      @Param("status") int status,
                      @Param("reason") String reason,
                      @Param("reviewerId") long reviewerId,
                      @Param("reviewTime") long reviewTime);
}