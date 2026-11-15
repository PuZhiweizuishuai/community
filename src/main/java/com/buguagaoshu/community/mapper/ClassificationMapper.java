package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Classification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 动态分类数据访问。删除分类前的「是否还有问题」校验也放在这里，
 * 保证判断和删除在同一个数据库视角下完成。
 */
@Mapper
public interface ClassificationMapper {

    @Select("select * from classification order by kind, sortOrder, id")
    List<Classification> getAll();

    @Select("select * from classification where kind=#{kind} and status=1 order by sortOrder, id")
    List<Classification> getEnabledByKind(@Param("kind") String kind);

    @Select("select * from classification where id=#{id}")
    Classification selectById(@Param("id") long id);

    @Select("select * from classification where kind=#{kind} and code=#{code}")
    Classification selectByCode(@Param("kind") String kind, @Param("code") int code);

    @Select("select * from classification where kind=#{kind} and name=#{name}")
    Classification selectByName(@Param("kind") String kind, @Param("name") String name);

    @Select("select coalesce(max(code), 0) from classification where kind=#{kind}")
    int maxCode(@Param("kind") String kind);

    @Insert("insert into classification(kind, code, name, sortOrder, status, createTime, modifiedTime) " +
            "values (#{kind}, #{code}, #{name}, #{sortOrder}, #{status}, #{createTime}, #{modifiedTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Classification classification);

    @Update("update classification set code=#{code}, name=#{name}, sortOrder=#{sortOrder}, " +
            "status=#{status}, modifiedTime=#{modifiedTime} where id=#{id}")
    int update(Classification classification);

    @Delete("delete from classification where id=#{id}")
    int deleteById(@Param("id") long id);

    // ------------------------------------------------------------------
    // 删除前的使用情况校验
    // ------------------------------------------------------------------

    /** 问题分类：Questions.classification 存的是分类名称 */
    @Select("select count(*) from Questions where classification=#{name}")
    long countQuestionsByClassification(@Param("name") String name);

    /** 话题分类：统计归类到该分类下所有话题的帖子数（兼容中英文逗号分隔的标签） */
    @Select("select count(*) from Questions q where exists (" +
            "select 1 from topic t where t.type=#{code} " +
            "and find_in_set(t.title, replace(q.tag, '，', ',')) > 0)")
    long countQuestionsByTopicType(@Param("code") int code);

    @Select("select count(*) from topic where type=#{code}")
    long countTopicsByType(@Param("code") int code);

    // ------------------------------------------------------------------
    // 重命名 / 改编码时同步历史数据，避免产生孤儿数据
    // ------------------------------------------------------------------

    @Update("update Questions set classification=#{newName} where classification=#{oldName}")
    int renameQuestionClassification(@Param("oldName") String old, @Param("newName") String name);

    @Update("update topic set type=#{newCode} where type=#{oldCode}")
    int renameTopicType(@Param("oldCode") int old, @Param("newCode") int code);
}
