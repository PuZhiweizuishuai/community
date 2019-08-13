package com.buguagaoshu.community.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 18:01
 */
public class StringUtil {
    /**
     * 返回处理结果
     */
    public static HashMap<String, Object> dealResultMessage(boolean success, String msg) {
        HashMap<String, Object> hashMap = new HashMap<>(2);
        hashMap.put("success", success);
        hashMap.put("msg", msg);
        return hashMap;
    }


    /**
     * 获取当前系统时间
     */
    public static String getNowTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return simpleDateFormat.format(now);
    }

    /**
     * 根据生日，计算年龄
     *
     * @param birthday 格式化的生日字符串
     * @return -1 年龄输入错误
     */
    public static int getAge(String birthday) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        String nowTime = simpleDateFormat.format(now);
        String[] userBirthday = birthday.split("-");
        String[] nowDay = nowTime.split("-");
        int age = Integer.valueOf(nowDay[0]) - Integer.valueOf(userBirthday[0]);
        if (age > 0) {
            return age;
        } else if (age == 0) {
            int month = Integer.valueOf(nowDay[1]) - Integer.valueOf(userBirthday[1]);
            if (month > 0) {
                return 0;
            } else if (month == 0) {
                int day = Integer.valueOf(nowDay[2]) - Integer.valueOf(userBirthday[2]);
                if (day > 0) {
                    return 0;
                } else if (day == 0) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }

        } else {
            return -1;
        }
    }

}
