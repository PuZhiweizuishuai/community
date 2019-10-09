package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.TagClass;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-09 17:34
 */
public interface TagClassService {
    PaginationDto<TagClass> getTagClassByType(String type, String page, String size);
}
