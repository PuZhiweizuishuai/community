package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 13:16
 */
@Mapper
@Component(value = "UserMapper")
public interface UserMapper {
    /**
     * 通过 id 查询学生信息
     *
     * @param id 学号
     * @return 学生类
     */
    @Select("select * from users where id=#{id}")
    User selectUserById(long id);

    /**
     * 通过 email 查询学生信息
     *
     * @param email 学号
     * @return 学生类
     */
    @Select("select * from users where email=#{email}")
    User selectUserByEmail(String email);

    /**
     * 通过 userId 查询学生信息
     *
     * @param userId 用户名
     * @return 学生类
     */
    @Select("select * from users where userId=#{userId}")
    User selectUserByUserId(String userId);

    /**
     * 通过用户 ID 删除用户信息
     *
     * @param id 用户 ID
     * @return 1
     */
    @Delete("delete from users where id=#{id}")
    int deleteUserById(long id);

    /**
     * 添加新用户
     *
     * @param user 新用户类
     * @return 1
     */
    @Insert("insert into users(userId ,userName, password, email, sex, age, birthday, school, major, creationTime, lastTime, headUrl, userTopPhotoUrl) " +
            "values(#{userId}, #{userName}, #{password}, #{email}, #{sex}, #{age}, #{birthday}, #{school}, #{major}, #{creationTime}, #{lastTime}, #{headUrl}, #{userTopPhotoUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    /**
     * 通过 id 更改用户名
     *
     * @param id          用户 ID
     * @param newUserName 新用户名
     * @return 1 成功
     */
    @Update("update users set userName=#{newUserName} where id=#{id}")
    int updateUserNameById(@Param("id") long id, @Param("newUserName") String newUserName);

    /**
     * 通过 id 更改邮箱
     *
     * @param id       用户 ID
     * @param newEmail 新邮箱
     * @return 1 成功
     */
    @Update("update users set email=#{newEmail} where id=#{id}")
    int updateUserEmailById(@Param("id") long id, @Param("newEmail") String newEmail);

    /**
     * 通过 id 密码
     *
     * @param id          用户 ID
     * @param newPassword 新密码
     * @return 1 成功
     */
    @Update("update users set password=#{newPassword} where id=#{id}")
    int updateUserPasswordById(@Param("id") long id, @Param("newPassword") String newPassword);

    /**
     * 通过 id 更改登陆时间
     *
     * @param id       用户 ID
     * @param lastTime 上次登陆时间
     * @return 1 成功
     */
    @Update("update users set lastTime=#{lastTime} where id=#{id}")
    int updateUserLastTimeById(@Param("id") long id, @Param("lastTime") String lastTime);

    /**
     * 通过 id 更改用户头像
     *
     * @param id      用户 ID
     * @param headUrl 新头像地址
     * @return 1 成功
     */
    @Update("update users set headUrl=#{headUrl} where id=#{id}")
    int updateUserHeadUrlById(@Param("id") long id, @Param("headUrl") String headUrl);


    /**
     * 通过 id 更改用户性别
     *
     * @param id  用户 ID
     * @param sex 性别
     * @return 1 成功
     */
    @Update("update users set sex=#{sex} where id=#{id}")
    int updateUserSexById(@Param("id") long id, @Param("sex") String sex);

    /**
     * 通过 id 更改用户学校
     *
     * @param id     用户 ID
     * @param school 学校
     * @return 1 成功
     */
    @Update("update users set school=#{school} where id=#{id}")
    int updateUserSchoolById(@Param("id") long id, @Param("school") String school);


    /**
     * 通过 id 更改用户专业
     *
     * @param id    用户 ID
     * @param major 专业
     * @return 1 成功
     */
    @Update("update users set major=#{major} where id=#{id}")
    int updateUserMajorById(@Param("id") long id, @Param("major") String major);


    /**
     * 通过 id 更改用户简介
     *
     * @param id               用户 ID
     * @param selfIntroduction 专业
     * @return 1 成功
     */
    @Update("update users set selfIntroduction=#{selfIntroduction} where id=#{id}")
    int updateUserSelfIntroductionById(@Param("id") long id, @Param("selfIntroduction") String selfIntroduction);


    @Update("update users set userName=#{userName}, sex=#{sex}, school=#{school}, major=#{major}, simpleSelfIntroduction=#{simpleSelfIntroduction}, " +
            "selfIntroduction=#{selfIntroduction}, likes=#{likes} where id=#{id}")
    int updateUserData(User user);



    @Update("update users set userTopPhotoUrl=#{userTopPhotoUrl} where id=#{id}")
    int updateUserTopPhotoUrl(@Param("id") long id, @Param("userTopPhotoUrl") String userTopPhotoUrl);



    @Select("select * from users where userName regexp #{search} or userId regexp #{search} or simpleSelfIntroduction regexp #{search} or selfIntroduction regexp #{search} order by id desc limit #{page}, #{size}")
    List<User> searchUser(@Param("search") String search, @Param("page") long page, @Param("size") long size);


    @Select("select COUNT(*) from users where userName regexp #{search} or userId regexp #{search} or simpleSelfIntroduction regexp #{search} or selfIntroduction regexp #{search}")
    Long searchUserCount(@Param("search") String search);
}
