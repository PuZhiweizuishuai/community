package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "system_configs", camelToUnderline = false)
public class SystemConfig {
    @Id(keyType = KeyType.None)
    private String configKey;
    private String configValue;
    private Long modifiedTime;
}