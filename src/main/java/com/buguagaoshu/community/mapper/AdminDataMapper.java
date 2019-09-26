package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.AdminData;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 18:48
 */
@Mapper
public interface AdminDataMapper {
    @Insert("insert into admin(time, questionCount, userCount, userAddCount, questionAddCount) values " +
            "(#{time}, #{questionCount}, #{userCount}, #{userAddCount}, #{questionAddCount})")
    @Options(useGeneratedKeys = true, keyProperty = "adminId", keyColumn = "adminId")
    int insertAdminData(AdminData data);


    @Select("select COUNT(*) from admin")
    long getAllAdminDataCount();


    @Select("select * from admin order by adminId desc limit 1")
    AdminData getBestNetAdminData();


    @Select("select * from admin order by adminId desc limit #{page}, #{size}")
    List<AdminData> selectAdmminData(@Param("page") long page, @Param("size") long size);
}
