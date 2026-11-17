package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.StorageConfig;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component(value = "StorageConfigMapper")
public interface StorageConfigMapper extends BaseMapper<StorageConfig> {
}