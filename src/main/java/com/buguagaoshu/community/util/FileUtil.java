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

    /** 文件表/管理页使用的类型标签：image / video / audio / pdf / other */
    public static String typeLabel(String fileName) {
        return switch (getFileType(fileName)) {
            case IMAGE_FILE -> "image";
            case VIDEO_FILE -> "video";
            case MUSIC_FILE -> "audio";
            case PDF_FILE -> "pdf";
            default -> "other";
        };
    }

    /** 按扩展名推断 MIME，不信任客户端上报的 Content-Type */
    public static String contentType(String fileName) {
        String suffix = ImageUtil.getSuffix(fileName).toLowerCase();
        return switch (suffix) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".webp" -> "image/webp";
            case ".mp4" -> "video/mp4";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".pdf" -> "application/pdf";
            case ".zip" -> "application/zip";
            case ".rar" -> "application/vnd.rar";
            case ".7z" -> "application/x-7z-compressed";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".apk" -> "application/vnd.android.package-archive";
            default -> "application/octet-stream";
        };
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
