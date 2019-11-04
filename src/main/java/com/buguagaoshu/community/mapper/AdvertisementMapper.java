package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Advertisement;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 20:36
 */
@Mapper
public interface AdvertisementMapper {
    /**
     * 添加广告数据
     *
     * @param advertisement 广告
     * @return 插入结果
     */
    @Insert("insert into advertisement(title, url, image, createTime, modifiedUser, startTime, endTime, position, status) " +
            "values(#{title}, #{url}, #{image}, #{createTime}, #{modifiedUser}, #{startTime}, #{endTime}, #{position}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertAdvertisement(Advertisement advertisement);


    /**
     * 修改广告
     * @param advertisement 广告
     *  @return 插入结果
     * */
    @Update("update advertisement set title=#{title}, url=#{url}, image=#{image}, modifiedUser=#{modifiedUser} where id=#{id}")
    int alterAdvertisement(Advertisement advertisement);

    /**
     * 设置广告投放
     * @param advertisement 广告
     * @return 结果
     * */
    @Update("update advertisement set startTime=#{startTime}, endTime=#{endTime}, status=#{status}, modifiedUser=#{modifiedUser}, position=#{position} where id=#{id}")
    int updateAdvertisementSetting(Advertisement advertisement);


    /**
     * 查找广告列表
     * @param page 页码
     * @param size 数量
     * @return 结果
     */
    @Select("select * from advertisement order by id desc limit #{page}, #{size}")
    List<Advertisement> selectAdvertisementList(@Param("page") long page, @Param("size") long size);

    /**
     * 依据状态查找广告列表
     * @param status 状态
     * @return 结果
     * */
    @Select("select * from advertisement where status=#{status} order by id desc")
    List<Advertisement> selectAdvertisementByStatus(@Param("status") int status);

    /**
     * @return 所有广告数目
     * */
    @Select("select COUNT(*) from advertisement")
    long selectAdvertisementCount();

    /**
     * 通过广告id查找广告
     * @param id 广告id
     * @return 广告
     * */
    @Select("select * from advertisement where id=#{id}")
    Advertisement selectAdvertisementById(@Param("id") long id);


    /**
     * 更新阅读数
     * @param advertisement 广告
     * @return 结果
     * */
    @Update("update advertisement set viewCount=viewCount+#{viewCount} where id=#{id}")
    int updateAdvertisementViewCount(Advertisement advertisement);
}
