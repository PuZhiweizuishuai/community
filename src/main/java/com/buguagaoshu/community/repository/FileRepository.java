package com.buguagaoshu.community.repository;

import com.buguagaoshu.community.dto.FileRecordDto;
import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.FileRecordMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.FileRecord;
import com.buguagaoshu.community.model.StorageConfig;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.util.FileUtil;
import com.buguagaoshu.community.util.ImageUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.buguagaoshu.community.model.table.FileRecordTableDef.FILE_RECORD;

/**
 * 文件读写编排：上传校验、存储选择（启用多个时随机）、落文件表、统一读取与删除。
 * Controller 只负责协议适配，业务逻辑集中在这里。
 */
@Repository
public class FileRepository {

    private static final Logger log = LoggerFactory.getLogger(FileRepository.class);

    /** 读取结果：本地返回资源流，对象存储返回预签名重定向地址 */
    public record StoredFile(Resource resource, String redirectUrl, String contentType, String downloadName) {
    }

    private final StorageConfigRepository storageConfigRepository;
    private final FileRecordMapper fileRecordMapper;
    private final UserMapper userMapper;

    public FileRepository(StorageConfigRepository storageConfigRepository,
                          FileRecordMapper fileRecordMapper,
                          UserMapper userMapper) {
        this.storageConfigRepository = storageConfigRepository;
        this.fileRecordMapper = fileRecordMapper;
        this.userMapper = userMapper;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "没有收到文件");
        }
        String fileName = file.getOriginalFilename();
        if (!FileUtil.checkFileType(fileName)) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "不支持的文件格式");
        }
        if (ImageUtil.checkFileType(fileName)) {
            try {
                // 魔数校验：后缀可伪造，文件头必须是真实图片
                if (!ImageUtil.isImageContent(file.getBytes())) {
                    throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "图片内容不合法");
                }
            } catch (CustomizeException e) {
                throw e;
            } catch (Exception e) {
                throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "文件读取失败");
            }
        }
    }

    public FileRecord upload(MultipartFile file, User uploader) {
        validate(file);
        List<StorageConfig> enabled = storageConfigRepository.enabledConfigs();
        if (enabled.isEmpty()) {
            throw new CustomizeException(CustomizeErrorCode.SYS_ERROR.getCode(), "没有可用的存储，请先在后台启用存储配置");
        }
        // 启用多个存储时每次上传随机选择一个
        StorageConfig chosen = enabled.get(ThreadLocalRandom.current().nextInt(enabled.size()));
        FileStorageRepository repository = storageConfigRepository.repositoryFor(chosen.getId());
        if (repository == null) {
            throw new CustomizeException(CustomizeErrorCode.SYS_ERROR.getCode(), "存储配置不可用，请检查后台存储配置");
        }

        String originalName = file.getOriginalFilename();
        String key = uploader.getUserId() + "/" + ImageUtil.createNewFileName(originalName);
        try {
            repository.save(key, file);
        } catch (Exception e) {
            log.error("文件保存失败，storageId={}, key={}: {}", chosen.getId(), key, e.getMessage());
            throw new CustomizeException(CustomizeErrorCode.SYS_ERROR.getCode(), "上传失败，请重试");
        }

        FileRecord record = new FileRecord();
        record.setStorageId(chosen.getId());
        record.setFileKey(key);
        record.setOriginalName(originalName);
        record.setFileSize(file.getSize());
        record.setContentType(FileUtil.contentType(originalName));
        record.setFileType(FileUtil.typeLabel(originalName));
        record.setUrl("/api/file/" + chosen.getId() + "/" + key);
        record.setUploaderId(uploader.getId());
        record.setStatus(FileRecord.STATUS_NORMAL);
        record.setCreateTime(System.currentTimeMillis());
        fileRecordMapper.insert(record);
        return record;
    }

    /** 统一读取入口；存储或文件不存在返回 null */
    public StoredFile resolve(long storageId, String key) {
        String normalized = FileStorageRepository.normalizeKey(key);
        if (normalized.isEmpty()) {
            return null;
        }
        FileStorageRepository repository = storageConfigRepository.repositoryFor(storageId);
        if (repository == null) {
            return null;
        }
        if (repository.preferRedirect()) {
            try {
                return new StoredFile(null, repository.directUrl(normalized), null, null);
            } catch (Exception e) {
                log.warn("生成对象存储访问链接失败，storageId={}, key={}: {}", storageId, normalized, e.getMessage());
                return null;
            }
        }
        Resource resource = repository.load(normalized);
        if (resource == null) {
            return null;
        }
        FileRecord record = fileRecordMapper.selectOneByQuery(QueryWrapper.create()
                .where(FILE_RECORD.STORAGE_ID.eq(storageId))
                .and(FILE_RECORD.FILE_KEY.eq(normalized)));
        String downloadName = record != null && StringUtils.isNotBlank(record.getOriginalName())
                ? record.getOriginalName()
                : normalized.substring(normalized.lastIndexOf('/') + 1);
        return new StoredFile(resource, null, FileUtil.contentType(normalized), downloadName);
    }

    /** 删除物理文件并把记录标记为已删除（保留审计） */
    public void deleteById(long id) {
        FileRecord record = fileRecordMapper.selectOneById(id);
        if (record == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "文件不存在");
        }
        if (record.getStatus() != null && record.getStatus() == FileRecord.STATUS_DELETED) {
            return;
        }
        FileStorageRepository repository = storageConfigRepository.repositoryFor(record.getStorageId());
        if (repository != null && !repository.delete(record.getFileKey())) {
            throw new CustomizeException(CustomizeErrorCode.SYS_ERROR.getCode(), "物理文件删除失败，请检查存储配置");
        }
        FileRecord update = UpdateEntity.of(FileRecord.class, id);
        update.setStatus(FileRecord.STATUS_DELETED);
        fileRecordMapper.update(update);
    }

    public Page<FileRecordDto> list(long page, long size, String keyword, String fileType,
                                    Long storageId, Integer status) {
        QueryWrapper query = QueryWrapper.create();
        if (StringUtils.isNotBlank(keyword)) {
            query.and(FILE_RECORD.ORIGINAL_NAME.like(keyword).or(FILE_RECORD.FILE_KEY.like(keyword)));
        }
        if (StringUtils.isNotBlank(fileType)) {
            query.and(FILE_RECORD.FILE_TYPE.eq(fileType));
        }
        if (storageId != null) {
            query.and(FILE_RECORD.STORAGE_ID.eq(storageId));
        }
        if (status != null) {
            query.and(FILE_RECORD.STATUS.eq(status));
        }
        query.orderBy(FILE_RECORD.CREATE_TIME.desc(), FILE_RECORD.ID.desc());
        Page<FileRecord> result = fileRecordMapper.paginate(page, size, query);

        Map<Long, String> storageNames = storageConfigRepository.list().stream()
                .collect(Collectors.toMap(StorageConfig::getId, StorageConfig::getName));
        List<Long> uploaderIds = result.getRecords().stream()
                .map(FileRecord::getUploaderId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> uploaders = uploaderIds.isEmpty() ? Map.of()
                : userMapper.selectListByIds(uploaderIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<FileRecordDto> records = result.getRecords().stream().map(record -> {
            FileRecordDto dto = new FileRecordDto();
            dto.setId(record.getId());
            dto.setStorageId(record.getStorageId());
            dto.setStorageName(storageNames.getOrDefault(record.getStorageId(), "未知存储"));
            dto.setFileKey(record.getFileKey());
            dto.setOriginalName(record.getOriginalName());
            dto.setFileSize(record.getFileSize());
            dto.setContentType(record.getContentType());
            dto.setFileType(record.getFileType());
            dto.setUrl(record.getUrl());
            dto.setUploaderId(record.getUploaderId());
            User uploader = record.getUploaderId() == null ? null : uploaders.get(record.getUploaderId());
            dto.setUploaderName(uploader == null ? null : uploader.getUserName());
            dto.setStatus(record.getStatus());
            dto.setCreateTime(record.getCreateTime());
            return dto;
        }).toList();
        return new Page<>(records, result.getPageNumber(), result.getPageSize(), result.getTotalRow());
    }
}