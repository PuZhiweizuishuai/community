package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.SystemConfig;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component(value = "SystemConfigMapper")
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
}