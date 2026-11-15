package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:35
 */
@Service
public class TagClassServiceImpl implements TagClassService {
    private final TagClassMapper tagClassMapper;

    private final TagClassCache tagClassCache;

    @Autowired
    public TagClassServiceImpl(TagClassMapper tagClassMapper, TagClassCache tagClassCache) {
        this.tagClassMapper = tagClassMapper;
        this.tagClassCache = tagClassCache;
    }

    @Override
    public PaginationDto<TagClass> getTagClassByType(String type, String page, String size) {

        return null;
    }

    @Override
    public List<TagClass> getTagClassByTypeNotUserPage(String type) {
        int typeCode = 1;
        try {
            typeCode = Integer.parseInt(type);
            // 话题分类已改为后台动态维护，不再受固定枚举数量限制
            if (typeCode <= 0) {
                typeCode = 1;
            }
        } catch (Exception e) {
            typeCode = 1;
        }
        List<TagClass> tagClassList = new ArrayList<>();
        // 定时任务尚未首次执行时缓存可能为空，直接从数据库兜底
        List<TagClass> cached = tagClassCache.getTagClassList();
        if (cached == null) {
            cached = tagClassMapper.getAllTagClass();
        }
        if (cached == null) {
            return tagClassList;
        }
        for (TagClass tagClass : cached) {
            if (tagClass.getType() == typeCode) {
                tagClassList.add(tagClass);
            }
        }
        return tagClassList;
    }

    @Override
    public int updateTalkCount(String tag, int count) {
        String regex = ",|，";
        String[] tags = tag.split(regex);
        if (tags.length == 1) {
            TagClass tagClass = tagClassCache.getTagClassMap().get(tag);


            if (tagClass != null) {
                tagClass.setTalkCount(tagClass.getTalkCount() + count);
                tagClassCache.getTagClassMap().put(tagClass.getTitle(), tagClass);
                tagClass.setTalkCount(count);
                tagClassMapper.updateTagTalkCount(tagClass);
                return 1;
            }
        }
        for (String tagStr : tags) {
            TagClass tagClass = tagClassCache.getTagClassMap().get(tagStr);
            if (tagClass != null) {
                tagClass.setTalkCount(tagClass.getTalkCount() + count);
                tagClassCache.getTagClassMap().put(tagClass.getTitle(), tagClass);
                tagClass.setTalkCount(count);
                tagClassMapper.updateTagTalkCount(tagClass);
            }
        }
        return 1;
    }

    @Override
    public int updateFollowCount(TagClass tagClass) {
        return tagClassMapper.updateTagFollowCount(tagClass);
    }

    @Override
    public int alterQuestionTalkCount(String[] newQuestionTag, String[] oldQuestionTag) {
        List<String> minus = StringUtil.minus(newQuestionTag, oldQuestionTag);
        if (minus.size() == 0) {
            return 0;
        }
        for (String string : newQuestionTag) {
            if (minus.contains(string)) {
                TagClass tagClass = tagClassCache.getTagClassMap().get(string);
                if (tagClass != null) {
                    tagClass.setTalkCount(tagClass.getTalkCount() + 1);
                    tagClassCache.getTagClassMap().put(tagClass.getTitle(), tagClass);
                    tagClass.setTalkCount(1);
                    tagClassMapper.updateTagTalkCount(tagClass);
                }
            }
        }

        for (String string : oldQuestionTag) {
            if (minus.contains(string)) {
                TagClass tagClass = tagClassCache.getTagClassMap().get(string);
                if (tagClass != null) {
                    tagClass.setTalkCount(tagClass.getTalkCount() - 1);
                    tagClassCache.getTagClassMap().put(tagClass.getTitle(), tagClass);
                    tagClass.setTalkCount(-1);
                    tagClassMapper.updateTagTalkCount(tagClass);
                }
            }
        }

        return 1;
    }
}
