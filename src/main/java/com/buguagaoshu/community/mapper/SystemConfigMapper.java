package com.buguagaoshu.community.mapper;

import com.buguagaoshu.community.model.SystemConfig;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component(value = "SystemConfigMapper")
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 配置项可能从未写入过（例如新增的聊天室开关），
     * BaseMapper#insertOrUpdate 在主键非空时只会执行 UPDATE，
     * 行不存在会静默影响 0 行，因此这里使用数据库原生 upsert。
     */
    @Insert("insert into system_configs(configKey, configValue, modifiedTime) " +
            "values(#{configKey}, #{configValue}, #{modifiedTime}) " +
            "on duplicate key update configValue=values(configValue), modifiedTime=values(modifiedTime)")
    int upsert(SystemConfig config);
}