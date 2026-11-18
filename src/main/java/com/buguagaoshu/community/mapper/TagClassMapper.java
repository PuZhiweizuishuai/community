package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TagClass;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

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
        update.setModifiedTime(tagClass.getModifiedTime());
        return update(update);
    }
}