package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.model.TagClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:41
 */
@Component
public class TagClassCache {
    private List<TagClass> tagClassList;

    private Map<String, TagClass> tagClassMap;

    public Map<String, TagClass> getTagClassMap() {
        return tagClassMap;
    }

    public void setTagClassMap(Map<String, TagClass> tagClassMap) {
        this.tagClassMap = tagClassMap;
    }

    public void setTagClassList(List<TagClass> tagClassList) {
        this.tagClassList = tagClassList;
    }

    public List<TagClass> getTagClassList() {
        return tagClassList;
    }
}
