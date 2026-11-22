package com.buguagaoshu.community.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-12 18:01
 */
public class StringUtil {

    /** BCrypt 编码器线程安全，全局单例（spring-security-crypto，无需整个 Spring Security） */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 密码规则：8~30 位，必须同时包含字母与数字，允许常用符号（同时供 @Pattern 声明式校验复用）。
     * 禁止纯数字/纯字母弱口令（如 12345678、password）。
     */
    public static final String PASSWORD_REGEX =
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d`~!@#$%^&*()+=|{}':;,\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“'。，、？]{8,30}$";

    /** 密码规则提示语，DTO 与前端共用同一文案 */
    public static final String PASSWORD_MESSAGE = "密码需为 8~30 位，且同时包含字母和数字";

    /**
     * 密码加密
     */
    public static String BCryptPassword(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    /**
     * 与数据库中的密码进行对比
     */
    public static boolean judgePassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }


    /**
     * 判断字符串是否为空
     */
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
     * 获取当前系统时间
     */
    public static String getNowTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return simpleDateFormat.format(now);
    }

    /**
     * 格式化时间
     */
    public static String foematTime(long data) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(data);
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
        try {
            int age = Integer.parseInt(nowDay[0]) - Integer.parseInt(userBirthday[0]);
            if (age > 0) {
                return age;
            } else if (age == 0) {
                int month = Integer.parseInt(nowDay[1]) - Integer.parseInt(userBirthday[1]);
                if (month > 0) {
                    return 0;
                } else if (month == 0) {
                    int day = Integer.parseInt(nowDay[2]) - Integer.parseInt(userBirthday[2]);
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
        } catch (Exception e) {
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
     * 清洗搜索关键字：限制长度并转义 LIKE 通配符，防止全表扫描放大与通配符注入。
     * 返回 null 表示关键字非法/为空。
     */
    public static String sanitizeSearchKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty() || trimmed.length() > 50) {
            return null;
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 清洗 FULLTEXT 布尔模式关键字：去掉布尔操作符，防止语法错误与恶意构造。
     * 返回 null 表示关键字非法/为空。
     */
    public static String sanitizeFulltextKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String cleaned = keyword.replaceAll("[+\\-><()~*\"@!\\\\]", " ")
                .replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty() || cleaned.length() > 50) {
            return null;
        }
        return cleaned;
    }

    /**
     * 邮箱脱敏，用于日志与审计：puzhiwei@foxmail.com → p******@foxmail.com。
     * 避免把用户完整邮箱写进日志文件。
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "******" + domain;
    }

    /**
     * 密码强度校验：至少 8 位，且同时包含字母与数字（可含常用符号）。
     * 与 {@link #PASSWORD_REGEX} 语义一致，供服务层二次校验使用。
     */
    public static boolean isStrongPassword(String password) {
        return password != null && password.matches(PASSWORD_REGEX);
    }

    public static String subString(String str, int n) {
        if (str.length() > n) {
            return str.substring(0, n) + "......";
        }
        return str;
    }
}
