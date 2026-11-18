package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.enums.QuestionClassType;
import com.buguagaoshu.community.mapper.ClassificationMapper;
import com.buguagaoshu.community.model.Classification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态分类缓存。后台增删改分类后调用 reload()，前台 /api/meta 立即拿到新列表。
 */
@Component
public class ClassificationCache {

    private final ClassificationMapper mapper;
    private volatile List<Classification> questionTypes = new ArrayList<>();
    private volatile List<Classification> topicTypes = new ArrayList<>();

    public ClassificationCache(ClassificationMapper mapper) {
        this.mapper = mapper;
    }

    public void reload() {
        List<Classification> all = mapper.getAll();
        List<Classification> questions = new ArrayList<>();
        List<Classification> topics = new ArrayList<>();
        if (all != null) {
            for (Classification item : all) {
                if (item.getStatus() == null || item.getStatus() != 1) {
                    continue;
                }
                if (Classification.KIND_QUESTION.equals(item.getKind())) {
                    questions.add(item);
                } else if (Classification.KIND_TOPIC.equals(item.getKind())) {
                    topics.add(item);
                }
            }
        }
        questionTypes = questions;
        topicTypes = topics;
    }

    public List<Classification> getQuestionTypes() {
        return questionTypes;
    }

    public List<Classification> getTopicTypes() {
        return topicTypes;
    }

    /** 首页 /?class={code} 使用；缓存为空时回退到旧枚举，保证兼容 */
    public String questionName(int code) {
        if (code == 0) {
            return null;
        }
        for (Classification item : questionTypes) {
            if (item.getCode() != null && item.getCode() == code) {
                return item.getName();
            }
        }
        return QuestionClassType.getNameStr(code);
    }

    public boolean isValidTopicCode(int code) {
        for (Classification item : topicTypes) {
            if (item.getCode() != null && item.getCode() == code) {
                return true;
            }
        }
        return false;
    }

    /** 校验问题类型 code 是否存在且启用 */
    public boolean isValidQuestionCode(Integer code) {
        if (code == null || code <= 0) {
            return false;
        }
        for (Classification item : questionTypes) {
            if (item.getCode() != null && item.getCode().intValue() == code) {
                return true;
            }
        }
        return false;
    }

    /** 问题类型 code → 名称（后台列表等只需要展示名称的场景） */
    public String questionTypeName(Integer code) {
        if (code == null) {
            return "";
        }
        for (Classification item : questionTypes) {
            if (item.getCode() != null && item.getCode().intValue() == code) {
                return item.getName();
            }
        }
        return QuestionClassType.getNameStr(code);
    }
}
