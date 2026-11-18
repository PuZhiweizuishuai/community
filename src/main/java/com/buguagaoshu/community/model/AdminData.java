package com.buguagaoshu.community.model;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 18:46
 * 管理数据
 */
@Data
@Table(value = "admin_snapshots", camelToUnderline = false)
public class AdminData {
    @Id(keyType = KeyType.Auto)
    private Long adminId;
    private Long time;
    private Long questionCount;
    private Long userCount;
    private Long userAddCount;
    private Long questionAddCount;
}
