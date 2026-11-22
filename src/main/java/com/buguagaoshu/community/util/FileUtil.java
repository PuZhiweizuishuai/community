package com.buguagaoshu.community.util;

import com.buguagaoshu.community.enums.FileTypeEnum;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-20 13:49
 */
public class FileUtil {

    /** 文件头识别出的真实类型（与后缀无关） */
    public enum MagicType {
        PDF, ZIP, OLE2, RAR, SEVEN_ZIP,
        JPEG, PNG, GIF, BMP, WEBP,
        MP4, MP3, WAV,
        /** 无法识别的文件头 */
        UNKNOWN
    }

    /** 读取文件头时需要的字节数（MP4 的 ftyp 在偏移 4~7，WAV/WEBP 在 8~11） */
    public static final int MAGIC_HEAD_LENGTH = 16;

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

    /**
     * 依据文件头（魔数）识别真实类型。
     * 覆盖本项目允许上传的全部后缀对应的容器格式，用于防止「改后缀伪装」。
     *
     * @param head 文件前若干字节（建议至少 {@link #MAGIC_HEAD_LENGTH} 字节）
     * @return 识别出的真实类型，无法识别返回 {@link MagicType#UNKNOWN}
     */
    public static MagicType detectMagicType(byte[] head) {
        if (head == null || head.length < 4) {
            return MagicType.UNKNOWN;
        }
        // PDF: 25 50 44 46 "%PDF"
        if (startsWith(head, "%PDF")) {
            return MagicType.PDF;
        }
        // ZIP / OOXML / APK: 50 4B 03 04（也兼容空包 50 4B 05 06、分卷 50 4B 07 08）
        if (startsWith(head, "PK")) {
            return MagicType.ZIP;
        }
        // OLE2（老版 doc/xls/ppt）: D0 CF 11 E0 A1 B1 1A E1
        if ((head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF && (head[2] & 0xFF) == 0x11
                && (head[3] & 0xFF) == 0xE0) {
            return MagicType.OLE2;
        }
        // RAR: 52 61 72 21 1A 07 "Rar!"
        if (startsWith(head, "Rar!")) {
            return MagicType.RAR;
        }
        // 7Z: 37 7A BC AF 27 1C "7z"
        if ((head[0] & 0xFF) == 0x37 && (head[1] & 0xFF) == 0x7A && (head[2] & 0xFF) == 0xBC
                && (head[3] & 0xFF) == 0xAF) {
            return MagicType.SEVEN_ZIP;
        }
        // JPEG: FF D8 FF
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return MagicType.JPEG;
        }
        // PNG: 89 50 4E 47
        if ((head[0] & 0xFF) == 0x89 && startsWith(head, "PNG", 1)) {
            return MagicType.PNG;
        }
        // GIF: GIF87a / GIF89a
        if (startsWith(head, "GIF8")) {
            return MagicType.GIF;
        }
        // BMP: BM
        if (startsWith(head, "BM")) {
            return MagicType.BMP;
        }
        // RIFF 容器：WEBP / WAV 需要看偏移 8 处的格式标识
        if (startsWith(head, "RIFF") && head.length >= 12) {
            if (startsWith(head, "WEBP", 8)) {
                return MagicType.WEBP;
            }
            if (startsWith(head, "WAVE", 8)) {
                return MagicType.WAV;
            }
            return MagicType.UNKNOWN;
        }
        // MP4: 偏移 4 起为 "ftyp"
        if (head.length >= 8 && startsWith(head, "ftyp", 4)) {
            return MagicType.MP4;
        }
        // MP3: "ID3" 标签或 MPEG 帧同步（FF Ex / FF Fx）
        if (startsWith(head, "ID3")) {
            return MagicType.MP3;
        }
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xE0) == 0xE0) {
            return MagicType.MP3;
        }
        return MagicType.UNKNOWN;
    }

    /**
     * 后缀与真实文件头是否匹配。
     *
     * <p>OOXML（docx/xlsx/pptx）与 APK 本质都是 ZIP 容器，因此都按 ZIP 校验；
     * 老版 Office（doc/xls/ppt）按 OLE2 校验。
     */
    public static boolean isMagicConsistent(String fileName, MagicType actual) {
        if (actual == null || actual == MagicType.UNKNOWN) {
            return false;
        }
        return switch (ImageUtil.getSuffix(fileName).toLowerCase()) {
            case ".pdf" -> actual == MagicType.PDF;
            case ".zip", ".apk", ".docx", ".xlsx", ".pptx" -> actual == MagicType.ZIP;
            case ".doc", ".xls", ".ppt" -> actual == MagicType.OLE2;
            case ".rar" -> actual == MagicType.RAR;
            case ".7z" -> actual == MagicType.SEVEN_ZIP;
            case ".png" -> actual == MagicType.PNG;
            case ".jpg", ".jpeg" -> actual == MagicType.JPEG;
            case ".gif" -> actual == MagicType.GIF;
            case ".bmp" -> actual == MagicType.BMP;
            case ".webp" -> actual == MagicType.WEBP;
            case ".mp4" -> actual == MagicType.MP4;
            case ".mp3" -> actual == MagicType.MP3;
            case ".wav" -> actual == MagicType.WAV;
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, String ascii) {
        return startsWith(data, ascii, 0);
    }

    private static boolean startsWith(byte[] data, String ascii, int offset) {
        if (data == null || data.length < offset + ascii.length()) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if ((data[offset + i] & 0xFF) != ascii.charAt(i)) {
                return false;
            }
        }
        return true;
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
