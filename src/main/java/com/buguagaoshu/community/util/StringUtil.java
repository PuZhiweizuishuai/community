package com.buguagaoshu.community.util;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 18:01
 */
public class StringUtil {
    /**
     * 密码加密
     */
    public static String BCryptPassword(String password) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.encode(password);
    }

    /**
     * 与数据库中的密码进行对比
     */
    public static boolean judgePassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
    }


    /**
     * 判断字符串是否为空
     * */
    public static boolean isEmpty(String str) {
        if (str == null) {
            return true;
        } else {
            if (str.equals("")) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 验证密码格式
     */
    public static boolean checkPassword(String password) {
        String regx = "^([A-Z]|[a-z]|[0-9]|[`~!@#$%^&*()+=|{}':;',\\\\\\\\[\\\\\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“'。，、？]){6,30}$";
        return password.matches(regx);
    }

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

    /**
     * 验证邮箱
     */
    public static boolean checkEmail(String email) {
        String reg = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
        return email.matches(reg);
    }

    /**
     * 验证用户 ID
     */
    public static boolean checkUserId(String userId) {
        return userId.matches("^\\w+$");
    }

    /**
     * 获取一个UUID
     */
    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    /**
     * 页面跳转的语言参数
     *
     * @param url        要跳转的页面
     * @param isRedirect 是否重定向
     */
    public static String jumpWebLangeParameter(String url, boolean isRedirect,
                                               HttpServletRequest request) {
        StringBuffer stringBuffer = new StringBuffer();
        if (isRedirect) {
            stringBuffer.append("redirect:");
        }
        String l = request.getParameter("l");
        if (l != null && l.equals("en-US")) {

            stringBuffer.append(url);
            stringBuffer.append("?l=en-US");
        } else {
            stringBuffer.append(url);
        }
        return stringBuffer.toString();
    }
}
