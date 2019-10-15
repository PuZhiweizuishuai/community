package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.cache.TagClassCache;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.enums.TagClassTypeEnum;
import com.buguagaoshu.community.mapper.TagClassMapper;
import com.buguagaoshu.community.model.TagClass;
import com.buguagaoshu.community.service.TagClassService;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
            typeCode = Integer.valueOf(type);
            if (typeCode <= 0 || typeCode > TagClassTypeEnum.values().length) {
                typeCode = 1;
            }
        } catch (Exception e) {
            typeCode = 1;
        }
        List<TagClass> tagClassList = new ArrayList<>();
        for (TagClass tagClass : tagClassCache.getTagClassList()) {
            if (tagClass.getType() == typeCode) {
                tagClassList.add(tagClass);
            }
        }
        return tagClassList;
    }

    @Override
    public int updateTalkCount(String tag) {
        String regex = ",|，";
        String[] tags = tag.split(regex);
        if (tags.length == 1) {
            TagClass tagClass = tagClassMapper.getTagClassByTitle(tag);


            if (tagClass != null) {
                tagClass.setTalkCount(1);
                tagClassMapper.updateTagTalkCount(tagClass);
                return 1;
            }
        }
        for (String tagStr : tags) {
            TagClass tagClass = tagClassMapper.getTagClassByTitle(tagStr);
            if (tagClass != null) {
                tagClass.setTalkCount(1);
                tagClassMapper.updateTagTalkCount(tagClass);
            }
        }
        return 1;
    }

    @Override
    public int alterQuestionTalkCount(String[] newQuestionTag, String[] oldQuestionTag) {
        List<String> minus = StringUtil.minus(newQuestionTag, oldQuestionTag);
        for(String string : newQuestionTag) {
            if(minus.contains(string)) {
                TagClass tagClass = tagClassMapper.getTagClassByTitle(string);
                if(tagClass != null) {
                    tagClass.setTalkCount(1);
                    tagClassMapper.updateTagTalkCount(tagClass);
                }
            }
        }

        for(String string : oldQuestionTag) {
            if(minus.contains(string)) {
                TagClass tagClass = tagClassMapper.getTagClassByTitle(string);
                if(tagClass != null) {
                    tagClass.setTalkCount(-1);
                    tagClassMapper.updateTagTalkCount(tagClass);
                }
            }
        }

        return 1;
    }
}
