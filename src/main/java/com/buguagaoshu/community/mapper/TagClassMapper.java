package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.TagClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * 通过话题类型获取话题数量
     * @param type 话题类型
     * @return 数量
     * */
    @Select("select COUNT(*) from topic where type=#{type}")
    long getTagClassCountByType(@Param("type") String type);
}
