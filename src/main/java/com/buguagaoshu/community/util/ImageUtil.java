package com.buguagaoshu.community.util;

import java.util.UUID;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 19:02
 */
public class ImageUtil {
    /**
     * 获取文件后缀名
     */
    public static String getSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 检查文件格式
     */
    public static boolean checkFileType(String fileName) {
        String str = getSuffix(fileName).toUpperCase();
        if (".PNG".equals(str) || ".JPG".equals(str) || ".JPEG".equals(str)
                || ".WEBP".equals(str) || ".BMP".equals(str) || ".GIF".equals(str)) {
            return true;
        }
        return false;
    }

    /**
     * 生成新的文件名
     */
    public static String createNewFileName(String fileOriginName) {
        return UUID.randomUUID() + getSuffix(fileOriginName);
    }
}
