package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.AdminDataDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.AdminData;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 20:17
 */
public interface AdminDataService {
    /**
     * 插入数据变化
     * */
    int insertAdminData(AdminData data);

    /**
     * 返回近期的数据
     * */
    PaginationDto<AdminDataDTO> selectAdminData(String page, String size);


    AdminData getBestNetAdminData();
}
