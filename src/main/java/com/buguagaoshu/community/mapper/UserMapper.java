package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.model.UserBanStatus;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 13:16
 */
@Mapper
@Component(value = "UserMapper")
public interface UserMapper extends BaseMapper<User> {

    /**
     * 添加新用户。id 为原始类型默认 -1，不能进入 INSERT 列（否则写入负主键），保留显式列 SQL。
     */
    @Insert("insert into users(userId ,userName, password, email, sex, age, birthday, school, major, creationTime, lastTime, headUrl, userTopPhotoUrl) " +
            "values(#{userId}, #{userName}, #{password}, #{email}, #{sex}, #{age}, #{birthday}, #{school}, #{major}, #{creationTime}, #{lastTime}, #{headUrl}, #{userTopPhotoUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertUser(User user);

    default int updateUserNameById(long id, String newUserName) {
        User update = UpdateEntity.of(User.class, id);
        update.setUserName(newUserName);
        return update(update);
    }

    default int updateUserEmailById(long id, String newEmail) {
        User update = UpdateEntity.of(User.class, id);
        update.setEmail(newEmail);
        return update(update);
    }

    default int updateUserPasswordById(long id, String newPassword) {
        User update = UpdateEntity.of(User.class, id);
        update.setPassword(newPassword);
        return update(update);
    }

    default int updateUserLastTimeById(long id, long lastTime) {
        User update = UpdateEntity.of(User.class, id);
        update.setLastTime(lastTime);
        return update(update);
    }

    default int updateUserHeadUrlById(long id, String headUrl) {
        User update = UpdateEntity.of(User.class, id);
        update.setHeadUrl(headUrl);
        return update(update);
    }

    default int updateUserSexById(long id, String sex) {
        User update = UpdateEntity.of(User.class, id);
        update.setSex(sex);
        return update(update);
    }

    default int updateUserSchoolById(long id, String school) {
        User update = UpdateEntity.of(User.class, id);
        update.setSchool(school);
        return update(update);
    }

    default int updateUserMajorById(long id, String major) {
        User update = UpdateEntity.of(User.class, id);
        update.setMajor(major);
        return update(update);
    }

    default int updateUserSelfIntroductionById(long id, String selfIntroduction) {
        User update = UpdateEntity.of(User.class, id);
        update.setSelfIntroduction(selfIntroduction);
        return update(update);
    }

    default int updateUserTopPhotoUrl(long id, String userTopPhotoUrl) {
        User update = UpdateEntity.of(User.class, id);
        update.setUserTopPhotoUrl(userTopPhotoUrl);
        return update(update);
    }

    default int updateUserData(User user) {
        User update = UpdateEntity.of(User.class, user.getId());
        update.setUserName(user.getUserName());
        update.setSex(user.getSex());
        update.setSchool(user.getSchool());
        update.setMajor(user.getMajor());
        update.setSimpleSelfIntroduction(user.getSimpleSelfIntroduction());
        update.setSelfIntroduction(user.getSelfIntroduction());
        update.setLikes(user.getLikes());
        return update(update);
    }

    /**
     * 更新获赞数（原子增量）
     */
    @Update("update users set likeCount=likeCount+#{likeCount} where id=#{id}")
    int updateUserLikeCount(@Param("likeCount") long likeCount, @Param("id") long id);

    /**
     * 更新关注数（原子增量）
     */
    @Update("update users set followCount=followCount+#{followCount} where id=#{id}")
    int updateUserFollowCount(User user);

    /**
     * 更新粉丝数（原子增量）
     */
    @Update("update users set fansCount=fansCount+#{fansCount} where id=#{id}")
    int updateUserFansCount(User user);

    // ------------------------------------------------------------------
    // 用户搜索：FULLTEXT(ngram) 布尔模式，保留 SQL
    // ------------------------------------------------------------------

    /** 搜索列表列：排除 password 与自我介绍大字段 */
    String SEARCH_COLUMNS = "id, userId, userName, email, age, sex, school, major, creationTime, lastTime, " +
            "headUrl, userTopPhotoUrl, simpleSelfIntroduction, followCount, fansCount, likeCount";

    @Select("select " + SEARCH_COLUMNS + " from users where MATCH(userName, userId, simpleSelfIntroduction, selfIntroduction) AGAINST(#{search} IN BOOLEAN MODE) order by id desc limit #{page}, #{size}")
    List<User> searchUser(@Param("search") String search, @Param("page") long page, @Param("size") long size);

    @Select("select COUNT(*) from users where MATCH(userName, userId, simpleSelfIntroduction, selfIntroduction) AGAINST(#{search} IN BOOLEAN MODE)")
    Long searchUserCount(@Param("search") String search);

    // ------------------------------------------------------------------
    // 用户封禁
    // ------------------------------------------------------------------

    @Select("SELECT id AS userId, banType, banEndTime, banReason FROM users WHERE id = #{userId}")
    UserBanStatus selectBanStatus(@Param("userId") long userId);

    @Select("""
            <script>
            SELECT id AS userId, banType, banEndTime, banReason
            FROM users
            WHERE id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<UserBanStatus> selectBanStatusByUserIds(@Param("userIds") Collection<Long> userIds);

    /** 当前生效的封禁用户数：永久封禁 + 临时封禁未到期 */
    @Select("SELECT COUNT(*) FROM users WHERE banType = 2 OR (banType = 1 AND banEndTime > #{now})")
    long countActiveBanned(@Param("now") long now);

    @Update("UPDATE users SET banType = #{banType}, banEndTime = #{banEndTime}, banReason = #{banReason}, "
            + "banTime = #{banTime}, banOperator = #{banOperator} WHERE id = #{userId}")
    int updateBanStatus(@Param("userId") long userId,
                        @Param("banType") int banType,
                        @Param("banEndTime") long banEndTime,
                        @Param("banReason") String banReason,
                        @Param("banTime") long banTime,
                        @Param("banOperator") long banOperator);
}