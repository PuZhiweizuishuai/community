package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TagClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:29
 */
@Mapper
public interface TagClassMapper {
    /**
     * @return 返回所有的话题
     * */
    @Select("select * from topic")
    List<TagClass> getAllTagClass();

    /**
     * 通过话题类型获取话题列表
     * @param type 话题类型
     * @return 结果
     * */
    @Select("select * from topic where type=#{type}")
    List<TagClass> getTagClassByType(@Param("type") String type);

    /**
     * @param title 问题标题
     * */
    @Select("select * from topic where title=#{title}")
    TagClass getTagClassByTitle(@Param("title") String title);

    /**
     * 通过话题类型获取话题数量
     * @param type 话题类型
     * @return 数量
     * */
    @Select("select COUNT(*) from topic where type=#{type}")
    long getTagClassCountByType(@Param("type") String type);

    /**
     * 更新讨论数
     * */
    @Update("update topic set talkCount=talkCount+#{talkCount} where id=#{id}")
    int updateTagTalkCount(TagClass tagClass);


    /**
     * 更新关注数
     * */
    @Update("update topic set followCount=followCount+#{followCount} where id=#{id}")
    int updateTagFollowCount(TagClass tagClass);
}