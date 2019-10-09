package com.buguagaoshu.community.cache;

import com.buguagaoshu.community.model.TagClass;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:41
 */
@Component
public class TagClassCache {
    private List<TagClass> tagClassList;

    public void setTagClassList(List<TagClass> tagClassList) {
        this.tagClassList = tagClassList;
    }

    public List<TagClass> getTagClassList() {
        return tagClassList;
    }
}
