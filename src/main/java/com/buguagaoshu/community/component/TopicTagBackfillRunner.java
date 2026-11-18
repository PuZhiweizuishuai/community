package com.buguagaoshu.community.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * V17 存量数据回填：把旧的「多话题标签串」迁移为「单选话题 + 多选标签」。
 *
 * <p>策略（与实施计划一致）：
 * <ul>
 *   <li>按 , / ，拆分 questions.tag；</li>
 *   <li>第一个命中 topics.title 的名称作为 topicId，其余命中话题降级为标签；</li>
 *   <li>未命中的自由文本全部创建为标签（存量直接 APPROVED，createdBy = NULL）；</li>
 *   <li>一个都没命中的帖子归入默认话题「综合讨论」；</li>
 *   <li>重算 topics.talkCount 与 tags.usageCount，最后把 topicId 收紧为 NOT NULL 并补外键。</li>
 * </ul>
 *
 * <p>幂等：只处理 topicId IS NULL 的帖子，重复执行不会重复建立关联。
 */
@Component
@Slf4j
@Order
public class TopicTagBackfillRunner implements ApplicationRunner {

    private static final String DEFAULT_TOPIC_TITLE = "综合讨论";
    private static final int MAX_TAG_LENGTH = 50;

    private final JdbcTemplate jdbcTemplate;

    public TopicTagBackfillRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!columnExists("questions", "topicId")) {
                log.warn("questions.topicId 不存在，跳过 V17 存量回填（请确认 Flyway V17 是否执行成功）");
                return;
            }
            Long defaultTopicId = ensureDefaultTopic();
            boolean migrated = migrateQuestions(defaultTopicId);
            if (migrated || !migrationMarked()) {
                recomputeCounters();
                markMigrationDone();
            }
            tightenTopicColumn();
        } catch (Exception e) {
            log.error("V17 存量回填失败，话题/标签相关功能可能不完整，可修复后重启重试", e);
        }
    }

    private Long ensureDefaultTopic() {
        List<Long> ids = jdbcTemplate.queryForList(
                "select id from topics where title = ? order by id limit 1", Long.class, DEFAULT_TOPIC_TITLE);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        jdbcTemplate.update("insert into topics(title, talkCount, followCount, image, simpleDesc, type, " +
                        "createTime, modifiedTime, status) values (?,0,0,?,?,?,?,?,1)",
                DEFAULT_TOPIC_TITLE, "/image/topicicon/default.svg",
                "暂时没有归属话题的讨论都会放在这里", 7,
                System.currentTimeMillis(), System.currentTimeMillis());
        return jdbcTemplate.queryForObject("select max(id) from topics", Long.class);
    }

    private boolean migrateQuestions(Long defaultTopicId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select questionId, tag from questions where topicId is null");
        if (rows.isEmpty()) {
            return false;
        }
        Map<String, Long> topicByTitle = loadTopicTitles();
        Map<String, Long> tagIds = loadTagIds();
        int migrated = 0;
        for (Map<String, Object> row : rows) {
            long questionId = ((Number) row.get("questionId")).longValue();
            String rawTag = row.get("tag") == null ? "" : String.valueOf(row.get("tag"));

            List<String> names = splitNames(rawTag);
            long topicId = 0L;
            Set<String> remaining = new LinkedHashSet<>();
            for (String name : names) {
                String key = name.toLowerCase(Locale.ROOT);
                Long matched = topicByTitle.get(key);
                if (topicId == 0 && matched != null) {
                    // 第一个命中话题的名称作为唯一话题，不再重复作为标签
                    topicId = matched;
                } else {
                    remaining.add(name);
                }
            }
            if (topicId == 0) {
                topicId = defaultTopicId;
            }

            List<Long> tagIdList = new ArrayList<>(remaining.size());
            for (String name : remaining) {
                Long tagId = tagIds.get(name.toLowerCase(Locale.ROOT));
                if (tagId == null) {
                    tagId = createApprovedTag(name);
                    tagIds.put(name.toLowerCase(Locale.ROOT), tagId);
                }
                tagIdList.add(tagId);
            }

            jdbcTemplate.update("update questions set topicId = ? where questionId = ?", topicId, questionId);
            for (Long tagId : tagIdList) {
                jdbcTemplate.update("insert ignore into question_tags(questionId, tagId) values (?, ?)",
                        questionId, tagId);
            }
            // 冗余列只保留已通过标签（回填阶段全部为已通过）
            jdbcTemplate.update("update questions set tag = ? where questionId = ?",
                    String.join(",", remaining), questionId);
            migrated++;
        }
        log.info("V17 存量回填完成：{} 个帖子已归属话题，默认话题 ID = {}", migrated, defaultTopicId);
        return true;
    }

    private Map<String, Long> loadTopicTitles() {
        Map<String, Long> result = new HashMap<>();
        List<Map<String, Object>> topics = jdbcTemplate.queryForList("select id, title from topics");
        for (Map<String, Object> row : topics) {
            if (row.get("title") == null) {
                continue;
            }
            // 同名话题只取最早创建的一条
            result.putIfAbsent(String.valueOf(row.get("title")).trim().toLowerCase(Locale.ROOT),
                    ((Number) row.get("id")).longValue());
        }
        return result;
    }

    private Map<String, Long> loadTagIds() {
        Map<String, Long> result = new HashMap<>();
        List<Map<String, Object>> tags = jdbcTemplate.queryForList("select id, name from tags");
        for (Map<String, Object> row : tags) {
            result.put(String.valueOf(row.get("name")).trim().toLowerCase(Locale.ROOT),
                    ((Number) row.get("id")).longValue());
        }
        return result;
    }

    private Long createApprovedTag(String name) {
        jdbcTemplate.update("insert ignore into tags(name, status, usageCount, createdBy, createTime) " +
                        "values (?, 1, 0, null, ?)",
                name, System.currentTimeMillis());
        return jdbcTemplate.queryForObject("select id from tags where name = ?", Long.class, name);
    }

    /** 拆分旧标签串：中英文逗号、去空、忽略大小写去重、过滤超长名称 */
    private List<String> splitNames(String raw) {
        Map<String, String> distinct = new LinkedHashMap<>();
        if (raw == null) {
            return List.of();
        }
        for (String part : raw.split("[,，]")) {
            String name = part == null ? "" : part.trim();
            if (name.isEmpty() || name.length() > MAX_TAG_LENGTH) {
                continue;
            }
            distinct.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        return new ArrayList<>(distinct.values());
    }

    /** 重算话题讨论数与标签使用数（只统计公开展示的帖子与全部关联） */
    private void recomputeCounters() {
        jdbcTemplate.update("update topics t set talkCount = (" +
                "select count(*) from questions q where q.topicId = t.id and q.status = 1 and q.reviewStatus = 1)");
        jdbcTemplate.update("update tags t set usageCount = (" +
                "select count(*) from question_tags qt where qt.tagId = t.id)");
    }

    /** topicId 收紧为 NOT NULL 并补外键（幂等） */
    private void tightenTopicColumn() {
        Long nullCount = jdbcTemplate.queryForObject(
                "select count(*) from questions where topicId is null", Long.class);
        if (nullCount != null && nullCount > 0) {
            log.warn("仍有 {} 个帖子没有 topicId，暂不收紧为非空约束", nullCount);
            return;
        }
        String nullable = jdbcTemplate.queryForObject(
                "select IS_NULLABLE from information_schema.columns " +
                        "where table_schema = database() and table_name = 'questions' and column_name = 'topicId'",
                String.class);
        if ("YES".equalsIgnoreCase(nullable)) {
            jdbcTemplate.execute("alter table questions modify column topicId bigint not null");
            log.info("questions.topicId 已收紧为 NOT NULL");
        }
        Long fkCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where table_schema = database() and table_name = 'questions' " +
                        "and constraint_name = 'fk_questions_topic'", Long.class);
        if (fkCount == null || fkCount == 0) {
            jdbcTemplate.execute("alter table questions add constraint fk_questions_topic " +
                    "foreign key (topicId) references topics(id)");
            log.info("questions.topicId 外键已创建");
        }
    }

    private boolean columnExists(String table, String column) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = database() and table_name = ? and column_name = ?",
                Long.class, table, column);
        return count != null && count > 0;
    }

    private boolean migrationMarked() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from system_configs where configKey = 'topic.migration.v17'", Long.class);
        return count != null && count > 0;
    }

    private void markMigrationDone() {
        jdbcTemplate.update("insert ignore into system_configs(configKey, configValue, modifiedTime) " +
                "values ('topic.migration.v17', 'done', ?)", System.currentTimeMillis());
    }
}
