package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.FileRecord;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import static com.buguagaoshu.community.model.table.FileRecordTableDef.FILE_RECORD;

@Mapper
@Component(value = "FileRecordMapper")
public interface FileRecordMapper extends BaseMapper<FileRecord> {

    /** 正常状态（未删除）的文件数 */
    default long countNormalFiles() {
        return selectCountByQuery(QueryWrapper.create()
                .where(FILE_RECORD.STATUS.eq(FileRecord.STATUS_NORMAL)));
    }

    /** 正常状态文件占用的存储空间（字节） */
    @Select("select coalesce(sum(fileSize), 0) from file_records where status = 0")
    long sumNormalFileSize();
}