package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.FileRecord;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component(value = "FileRecordMapper")
public interface FileRecordMapper extends BaseMapper<FileRecord> {
}