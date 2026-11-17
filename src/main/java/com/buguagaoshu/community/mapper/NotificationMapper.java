package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.Notification;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.buguagaoshu.community.model.table.NotificationTableDef.NOTIFICATION;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-02 23:25
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /** 未读总数 */
    default long getAllNotificationNoReadNumber(long receiver) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.STATUS.eq(0)));
    }

    /** 通知总数 */
    default long getAllNotificationNumber(long receiver) {
        return selectCountByQuery(QueryWrapper.create().where(NOTIFICATION.RECEIVER.eq(receiver)));
    }

    /** 未读回复数 */
    default long getNoReadCommentCount(long receiver) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.le(2)).and(NOTIFICATION.STATUS.eq(0)));
    }

    /** 未读点赞数 */
    default long getNoReadLikeCount(long receiver) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.in(3, 4)).and(NOTIFICATION.STATUS.eq(0)));
    }

    /** 未读系统通知数 */
    default long getNoReadSystemCount(long receiver) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.ge(5)).and(NOTIFICATION.STATUS.eq(0)));
    }

    /** 回复总数 */
    default long getAllCommentCount(long receiver) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.le(2)));
    }

    /** 点赞总数 */
    default long getAllLikeCount(long receiver) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.in(3, 4)));
    }

    /** 系统通知总数 */
    default long getAllSystemCount(long receiver) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.ge(5)));
    }

    /** 某类型未读数 */
    default long getNotificationAndTypeNumber(long receiver, int type) {
        return selectCountByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.STATUS.eq(0)).and(NOTIFICATION.TYPE.eq(type)));
    }

    /** 回复通知列表 */
    default List<Notification> getAllCommentNotification(long page, long size, long receiver) {
        return selectListByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.le(2))
                .orderBy(NOTIFICATION.ID.desc()).limit(page, size));
    }

    /** 点赞通知列表 */
    default List<Notification> getAllLikeNotification(long page, long size, long receiver) {
        return selectListByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.in(3, 4))
                .orderBy(NOTIFICATION.ID.desc()).limit(page, size));
    }

    /** 系统通知列表 */
    default List<Notification> getAllSystemNotification(long page, long size, long receiver) {
        return selectListByQuery(QueryWrapper.create()
                .where(NOTIFICATION.RECEIVER.eq(receiver)).and(NOTIFICATION.TYPE.ge(5))
                .orderBy(NOTIFICATION.ID.desc()).limit(page, size));
    }

    /** 设置消息已读 */
    default int readNotification(long id) {
        Notification update = UpdateEntity.of(Notification.class, id);
        update.setStatus(1);
        return update(update);
    }

    /** 一键已读 */
    default int readAll(long receiver) {
        Notification update = UpdateEntity.of(Notification.class);
        update.setStatus(1);
        return updateByQuery(update, QueryWrapper.create().where(NOTIFICATION.RECEIVER.eq(receiver)));
    }
}