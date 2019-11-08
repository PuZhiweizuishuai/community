package com.buguagaoshu.community.util;

import com.buguagaoshu.community.enums.FileTypeEnum;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-20 13:49
 */
public class FileUtil {
    public static boolean checkFileType(String fileName) {
        String type = ImageUtil.getSuffix(fileName).toUpperCase();
        return ".PNG".equals(type) || ".JPG".equals(type) || ".JPEG".equals(type)
                || ".WEBP".equals(type) || ".BMP".equals(type) || ".GIF".equals(type) || ".ZIP".equals(type)
                || ".RAR".equals(type) || ".7Z".equals(type) || ".DOC".equals(type) || ".DOCX".equals(type)
                || ".PDF".equals(type) || ".PPT".equals(type) || ".PPTX".equals(type) || ".XLS".equals(type)
                || ".XLSX".equals(type) || ".APK".equals(type) || ".MP4".equals(type) || ".WAV".equals(type)
                || ".MP3".equals(type);
    }

    public static FileTypeEnum getFileType(String fileName) {
        String type = ImageUtil.getSuffix(fileName).toUpperCase();
        if (".PNG".equals(type) || ".JPG".equals(type) || ".JPEG".equals(type)
                || ".WEBP".equals(type) || ".BMP".equals(type) || ".GIF".equals(type)) {
            return FileTypeEnum.IMAGE_FILE;
        }
        if(".MP4".equals(type)) {
            return FileTypeEnum.VIDEO_FILE;
        }

        if(".WAV".equals(type) || ".MP3".equals(type)) {
            return FileTypeEnum.MUSIC_FILE;
        }

        if (".PDF".equals(type)) {
            return FileTypeEnum.PDF_FILE;
        }

        return FileTypeEnum.FILE;
    }

    public static boolean checkLogFileType(String fileName) {
        int number = fileName.lastIndexOf(".");
        if (number <= 0) {
            return false;
        }
        String type = fileName.substring(number).toUpperCase();
        return ".LOG".equals(type) || ".GZ".equals(type);
    }

    public static boolean checkLogShow(String fileName) {
        int number = fileName.lastIndexOf(".");
        if (number <= 0) {
            return false;
        }
        String type = fileName.substring(number).toUpperCase();
        return ".LOG".equals(type);
    }

    public static boolean checkLogDelete(String fileName) {
        int number = fileName.lastIndexOf(".");
        // 安全检验，避免传入非法字符
        if (number <= 0 || fileName.contains("..") || fileName.contains("/")) {
            return false;
        }

        String type = fileName.substring(number).toUpperCase();
        return ".GZ".equals(type);
    }
}
