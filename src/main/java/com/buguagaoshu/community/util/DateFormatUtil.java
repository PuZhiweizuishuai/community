package com.buguagaoshu.community.util;


import java.text.SimpleDateFormat;

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

    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
        return SIMPLE_DATE_FORMAT.format(createTime);
    }
}
