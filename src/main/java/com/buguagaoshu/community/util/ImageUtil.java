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
        int number = fileName.lastIndexOf(".");
        if (number <= 0) {
            return "";
        }
        return fileName.substring(number);
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

    /**
     * 按文件头魔数校验图片内容（JPEG/PNG/GIF/BMP/WEBP），防止仅凭后缀伪造文件类型。
     */
    public static boolean isImageContent(byte[] data) {
        if (data == null || data.length < 12) {
            return false;
        }
        // JPEG: FF D8 FF
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            return true;
        }
        // GIF: GIF87a / GIF89a
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8') {
            return true;
        }
        // BMP: BM
        if (data[0] == 'B' && data[1] == 'M') {
            return true;
        }
        // WEBP: RIFF....WEBP
        return data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
    }
}
