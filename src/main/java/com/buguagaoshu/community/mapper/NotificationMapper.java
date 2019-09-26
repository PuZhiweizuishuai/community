package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Notification;
import org.apache.ibatis.annotations.*;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 23:25
 */
@Mapper
public interface NotificationMapper {
    /**
     * 插入消息通知
     * @param notification 消息
     * @return 插入结果
     * */
    @Insert("insert into notification(notifier, receiver, outerId, type, createTime, status)" +
            " values (#{notifier}, #{receiver}, #{outerId}, #{type}, #{createTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long insertNotification(Notification notification);


    @Delete("DELETE FROM notification where id=#{id}")
    int deleteNotification(@Param("id") long id);

    /**
     * 获取该用户当前所有未读通知数
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and status=0")
    long getAllNotificationNoReadNumber(long receiver);


    /**
     * 获取该用户当前所有通知数
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver}")
    long getAllNotificationNumber(long receiver);

    /**
     * 获取该用户当前回复数量
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and type<=2 and status=0")
    long getNoReadCommentCount(long receiver);


    /**
     * 获取该用户当前点赞数量
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and (type=3 or type=4) and status=0")
    long getNoReadLikeCount(long receiver);

    /**
     * 获取该用户当前所有系统通知数
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and type>=5 and status=0")
    long getNoReadSystemCount(long receiver);


    /**
     * 获取该用户当前回复数量
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and type<=2 ")
    long getAllCommentCount(long receiver);


    /**
     * 获取该用户当前点赞数量
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and (type=3 or type=4)")
    long getAllLikeCount(long receiver);

    /**
     * 获取该用户当前所有系统通知数
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and type>=5")
    long getAllSystemCount(long receiver);


    /**
     *获取某种通知的数量
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and status=0 and type=#{type}")
    long getNotificationAndTypeNumber(@Param("receiver") long receiver, @Param("type") int type);


    /**
     * 返回消息通知列表
     * */
    @Select("select * from notification where receiver=#{receiver} and type<=2 ORDER BY id DESC limit #{page}, #{size}")
    List<Notification> getAllCommentNotification(@Param("page") long page, @Param("size") long size, @Param("receiver") long receiver);


    /**
     * 返回所有点赞列表
     * */
    @Select("select * from notification where receiver=#{receiver} and (type=3 or type=4) ORDER BY id DESC limit #{page}, #{size}")
    List<Notification> getAllLikeNotification(@Param("page") long page, @Param("size") long size, @Param("receiver") long receiver);


    /**
     * 返回所有系统消息列表
     * */
    @Select("select * from notification where receiver=#{receiver} and type>=5  ORDER BY id DESC limit #{page}, #{size}")
    List<Notification> getAllSystemNotification(@Param("page") long page, @Param("size") long size, @Param("receiver") long receiver);



    @Select("select * from notification where id=#{id}")
    Notification selectNotificationById(long id);


    /**
     * 设置消息已读
     * */
    @Update("update notification set status=1 where id=#{id}")
    int readNotification(long id);


    /**
     * 一键已读
     * */
    @Update("update notification set status=1 where receiver=#{receiver}")
    int readAll(long receiver);
}
