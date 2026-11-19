package com.buguagaoshu.community.repository;

import com.buguagaoshu.community.exception.CustomizeErrorCode;
import com.buguagaoshu.community.exception.CustomizeException;
import com.buguagaoshu.community.mapper.FileRecordMapper;
import com.buguagaoshu.community.mapper.StorageConfigMapper;
import com.buguagaoshu.community.model.FileRecord;
import com.buguagaoshu.community.model.StorageConfig;
import com.buguagaoshu.community.repository.impl.LocalFileStorageRepository;
import com.buguagaoshu.community.repository.impl.S3FileStorageRepository;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.buguagaoshu.community.model.table.FileRecordTableDef.FILE_RECORD;
import static com.buguagaoshu.community.model.table.StorageConfigTableDef.STORAGE_CONFIG;

/**
 * 存储配置管理 + 存储实例工厂。
 * 对象存储客户端按配置缓存复用，配置修改（modifiedTime 变化）后自动重建。
 */
@Repository
public class StorageConfigRepository {

    private record Cached(S3FileStorageRepository repository, Long modifiedTime) {
    }

    private final StorageConfigMapper storageConfigMapper;
    private final FileRecordMapper fileRecordMapper;
    private final LocalFileStorageRepository localRepository;
    private final Map<Long, Cached> s3Cache = new ConcurrentHashMap<>();

    public StorageConfigRepository(StorageConfigMapper storageConfigMapper,
                                   FileRecordMapper fileRecordMapper,
                                   LocalFileStorageRepository localRepository) {
        this.storageConfigMapper = storageConfigMapper;
        this.fileRecordMapper = fileRecordMapper;
        this.localRepository = localRepository;
    }

    public List<StorageConfig> list() {
        return storageConfigMapper.selectListByQuery(
                QueryWrapper.create().orderBy(STORAGE_CONFIG.ID.asc()));
    }

    public StorageConfig find(long id) {
        return storageConfigMapper.selectOneById(id);
    }

    public List<StorageConfig> enabledConfigs() {
        return storageConfigMapper.selectListByQuery(QueryWrapper.create()
                .where(STORAGE_CONFIG.ENABLED.eq(true))
                .orderBy(STORAGE_CONFIG.ID.asc()));
    }

    /** 按 id 取存储实例；读取不受 enabled 影响，停用只影响新上传 */
    public FileStorageRepository repositoryFor(long id) {
        if (id == StorageConfig.LOCAL_ID) {
            return localRepository;
        }
        StorageConfig config = find(id);
        if (config == null || !StorageConfig.TYPE_S3.equals(config.getType())) {
            return null;
        }
        Cached cached = s3Cache.get(id);
        if (cached != null && Objects.equals(cached.modifiedTime(), config.getModifiedTime())) {
            return cached.repository();
        }
        synchronized (this) {
            cached = s3Cache.get(id);
            if (cached != null && Objects.equals(cached.modifiedTime(), config.getModifiedTime())) {
                return cached.repository();
            }
            S3FileStorageRepository fresh = new S3FileStorageRepository(config);
            if (cached != null) {
                cached.repository().close();
            }
            s3Cache.put(id, new Cached(fresh, config.getModifiedTime()));
            return fresh;
        }
    }

    public long fileCount(long storageId) {
        return fileRecordMapper.selectCountByQuery(QueryWrapper.create()
                .where(FILE_RECORD.STORAGE_ID.eq(storageId))
                .and(FILE_RECORD.STATUS.eq(FileRecord.STATUS_NORMAL)));
    }

    /** 各存储配置的文件数，供列表页一次性装配 */
    public Map<Long, Long> fileCounts() {
        Map<Long, Long> counts = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : fileRecordMapper.countNormalGroupByStorageId()) {
            Object storageId = row.get("storageId");
            Object total = row.get("total");
            if (storageId instanceof Number id && total instanceof Number count) {
                counts.put(id.longValue(), count.longValue());
            }
        }
        return counts;
    }

    /**
     * 新增/修改配置。id=0 仅允许修改名称与启用状态；
     * secretKey 留空表示沿用已保存的密钥。
     */
    public StorageConfig save(StorageConfig incoming) {
        long now = System.currentTimeMillis();
        if (incoming.getId() != null && incoming.getId() == StorageConfig.LOCAL_ID) {
            StorageConfig local = find(StorageConfig.LOCAL_ID);
            if (local == null) {
                throw new CustomizeException(CustomizeErrorCode.NOT_FOUND);
            }
            if (StringUtils.isNotBlank(incoming.getName())) {
                local.setName(incoming.getName().trim());
            }
            local.setEnabled(!Boolean.FALSE.equals(incoming.getEnabled()));
            local.setModifiedTime(now);
            storageConfigMapper.update(local);
            return local;
        }

        validateS3(incoming);
        if (incoming.getId() == null) {
            if (StringUtils.isBlank(incoming.getSecretKey())) {
                throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写 SecretKey");
            }
            incoming.setType(StorageConfig.TYPE_S3);
            incoming.setCreateTime(now);
            incoming.setModifiedTime(now);
            storageConfigMapper.insert(incoming);
            return incoming;
        }

        StorageConfig existing = find(incoming.getId());
        if (existing == null || !StorageConfig.TYPE_S3.equals(existing.getType())) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "存储配置不存在");
        }
        existing.setName(incoming.getName().trim());
        existing.setEndpoint(incoming.getEndpoint().trim());
        existing.setRegion(StringUtils.defaultIfBlank(incoming.getRegion(), "us-east-1").trim());
        existing.setAccessKey(incoming.getAccessKey().trim());
        if (StringUtils.isNotBlank(incoming.getSecretKey())) {
            existing.setSecretKey(incoming.getSecretKey().trim());
        }
        existing.setBucket(incoming.getBucket().trim());
        existing.setPathStyleAccess(!Boolean.FALSE.equals(incoming.getPathStyleAccess()));
        existing.setUrlExpireSeconds(incoming.getUrlExpireSeconds() == null || incoming.getUrlExpireSeconds() <= 0
                ? 86400L : incoming.getUrlExpireSeconds());
        existing.setEnabled(!Boolean.FALSE.equals(incoming.getEnabled()));
        existing.setModifiedTime(now);
        storageConfigMapper.update(existing);
        evict(existing.getId());
        return existing;
    }

    public void delete(long id) {
        if (id == StorageConfig.LOCAL_ID) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "本地存储不能删除，只能停用");
        }
        StorageConfig config = find(id);
        if (config == null) {
            throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "存储配置不存在");
        }
        if (fileCount(id) > 0) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                    "该存储下仍有文件，请先在文件管理中删除对应文件");
        }
        storageConfigMapper.deleteById(id);
        evict(id);
    }

    /** 用给定配置做连接测试；secretKey 留空时沿用库中已存密钥 */
    public void test(StorageConfig incoming) {
        StorageConfig merged = incoming;
        if (incoming.getId() != null && incoming.getId() != StorageConfig.LOCAL_ID
                && StringUtils.isBlank(incoming.getSecretKey())) {
            StorageConfig existing = find(incoming.getId());
            if (existing == null) {
                throw new CustomizeException(CustomizeErrorCode.NOT_FOUND.getCode(), "存储配置不存在");
            }
            merged = copyForTest(incoming, existing.getSecretKey());
        }
        validateS3(merged);
        if (StringUtils.isBlank(merged.getSecretKey())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写 SecretKey");
        }
        try (S3FileStorageRepository repository = new S3FileStorageRepository(merged)) {
            repository.testConnection();
        } catch (CustomizeException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(),
                    "连接失败：" + e.getMessage());
        }
    }

    private StorageConfig copyForTest(StorageConfig incoming, String secretKey) {
        StorageConfig merged = new StorageConfig();
        merged.setId(incoming.getId());
        merged.setName(incoming.getName());
        merged.setType(StorageConfig.TYPE_S3);
        merged.setEndpoint(incoming.getEndpoint());
        merged.setRegion(incoming.getRegion());
        merged.setAccessKey(incoming.getAccessKey());
        merged.setSecretKey(secretKey);
        merged.setBucket(incoming.getBucket());
        merged.setPathStyleAccess(incoming.getPathStyleAccess());
        merged.setUrlExpireSeconds(incoming.getUrlExpireSeconds());
        merged.setEnabled(incoming.getEnabled());
        return merged;
    }

    private void validateS3(StorageConfig config) {
        if (StringUtils.isBlank(config.getName())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写存储名称");
        }
        if (StringUtils.isBlank(config.getEndpoint())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写服务地址 Endpoint");
        }
        if (StringUtils.isBlank(config.getAccessKey())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写 AccessKey");
        }
        if (StringUtils.isBlank(config.getBucket())) {
            throw new CustomizeException(CustomizeErrorCode.PARAM_ERROR.getCode(), "请填写 Bucket");
        }
    }

    private void evict(long id) {
        Cached removed = s3Cache.remove(id);
        if (removed != null) {
            removed.repository().close();
        }
    }
}