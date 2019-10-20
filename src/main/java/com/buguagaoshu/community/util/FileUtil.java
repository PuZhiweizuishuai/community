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

        return FileTypeEnum.FILE;
    }
}
