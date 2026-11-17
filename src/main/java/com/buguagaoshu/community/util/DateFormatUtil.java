package com.buguagaoshu.community.util;


import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-07 14:45
 */
public class DateFormatUtil {
    /**
     * 秒
     * */
    private final static long SECOND = 1000L;

    /**
     * 分钟
     * */
    private final static long MINUTE = 60000L;

    /**
     * 小时
     * */
    private final static long HOUR = 3600000L;

    /**
     * 天
     * */
    private final static long DAY = 86400000L;

    private final static  long FOUR_DAY = 345600000L;

    /** DateTimeFormatter 线程安全，替代静态共享的 SimpleDateFormat */
    private final static DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * 获取发帖时间差
     * */
    public static String getTimeDifference(long createTime, long modifiedTime) {
        long time = modifiedTime - createTime;
        if (time < MINUTE) {
            return (time / SECOND) + " 秒前";
        }
        if (time < HOUR) {
            return (time / MINUTE) + " 分钟前";
        }
        if (time < DAY) {
            return (time / HOUR) + " 小时前";
        }
        if (time < FOUR_DAY) {
            return (time / DAY) + " 天前";
        }
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(createTime));
    }
}
