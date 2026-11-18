package com.buguagaoshu.community.component;

import com.buguagaoshu.community.cache.ClassificationCache;
import com.buguagaoshu.community.enums.QuestionClassType;
import com.buguagaoshu.community.enums.TagClassTypeEnum;
import com.buguagaoshu.community.mapper.ClassificationMapper;
import com.buguagaoshu.community.model.Classification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 动态分类的零停机初始化：老库没有 classifications 表时自动建表，
 * 并用原来写死在枚举里的分类做一次性种子数据；新库由 databases.sql 直接建表。
 *
 * <p>这样升级后不需要手工执行 DDL，已有的问题分类 / 话题分类也不会丢失。
 */
@Component
@Slf4j
public class ClassificationSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final ClassificationMapper mapper;
    private final ClassificationCache cache;

    public ClassificationSchemaInitializer(DataSource dataSource,
                                           ClassificationMapper mapper,
                                           ClassificationCache cache) {
        this.dataSource = dataSource;
        this.mapper = mapper;
        this.cache = cache;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS classifications (" +
                    " id bigint NOT NULL AUTO_INCREMENT," +
                    " kind varchar(20) NOT NULL," +
                    " code int NOT NULL," +
                    " name varchar(50) NOT NULL," +
                    " sortOrder int DEFAULT 0," +
                    " status int DEFAULT 1," +
                    " createTime bigint DEFAULT NULL," +
                    " modifiedTime bigint DEFAULT NULL," +
                    " PRIMARY KEY (id)," +
                    " UNIQUE KEY uk_classification_kind_code (kind, code)," +
                    " UNIQUE KEY uk_classification_kind_name (kind, name)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            long count = 0;
            try (ResultSet rs = statement.executeQuery("select count(*) from classifications")) {
                if (rs.next()) {
                    count = rs.getLong(1);
                }
            }
            if (count == 0) {
                seed();
                log.info("已初始化 {} 个问题分类与 {} 个话题分类",
                        QuestionClassType.values().length - 1, TagClassTypeEnum.values().length);
            }
            cache.reload();
        } catch (Exception e) {
            log.error("动态分类初始化失败，分类相关功能可能不可用", e);
        }
    }

    private void seed() {
        long now = System.currentTimeMillis();
        for (QuestionClassType type : QuestionClassType.values()) {
            if (type.getType() == 0) {
                continue;
            }
            insert(Classification.KIND_QUESTION, type.getType(), type.getName(), type.getType(), now);
        }
        for (TagClassTypeEnum type : TagClassTypeEnum.values()) {
            insert(Classification.KIND_TOPIC, type.getTypeCode(), type.getTypeName(), type.getTypeCode(), now);
        }
    }

    private void insert(String kind, int code, String name, int sortOrder, long now) {
        Classification item = new Classification();
        item.setKind(kind);
        item.setCode(code);
        item.setName(name);
        item.setSortOrder(sortOrder);
        item.setStatus(1);
        item.setCreateTime(now);
        item.setModifiedTime(now);
        mapper.insertSelective(item);
    }
}
