package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Notification;
import org.apache.ibatis.annotations.*;

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
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotification(Notification notification);

    /**
     * 获取该用户当前所有通知数
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and status=0")
    long getAllNotificationNumber(long receiver);

    /**
     *获取某种通知的数量
     * */
    @Select("select COUNT(1) from notification where receiver=#{receiver} and status=0 and type=#{type}")
    long getNotificationAndTypeNumber(@Param("receiver") long receiver, @Param("type") int type);


    /**
     * 返回消息通知列表
     * */
    @Select("select * from notification where receiver=#{receiver} ORDER BY id DESC limit #{page}, #{size}")
    List<Notification> getAllNotification(@Param("page") long page, @Param("size") long size, @Param("receiver") long receiver);


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
