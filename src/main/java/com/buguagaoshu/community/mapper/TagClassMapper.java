package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TagClass;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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

    // ------------------------------------------------------------------
    // 后台话题分类维护
    // ------------------------------------------------------------------

    @Select("select * from topic where id=#{id}")
    TagClass selectTagClassById(@Param("id") long id);

    /**
     * 新建话题。talkCount / followCount 为统计值，不允许后台直接写入。
     */
    @Insert("insert into topic(title, image, simpleDesc, type, status, createTime, modifiedTime) " +
            "values (#{title}, #{image}, #{simpleDesc}, #{type}, #{status}, #{createTime}, #{modifiedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTagClass(TagClass tagClass);

    /**
     * 编辑话题信息；标题、图标、简介、分类、状态可改，统计值保持不变。
     */
    @Update("update topic set title=#{title}, image=#{image}, simpleDesc=#{simpleDesc}, " +
            "type=#{type}, status=#{status}, modifiedTime=#{modifiedTime} where id=#{id}")
    int updateTagClass(TagClass tagClass);

    @Delete("delete from topic where id=#{id}")
    int deleteTagClassById(@Param("id") long id);
}