package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.AdminDataDTO;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.mapper.AdminDataMapper;
import com.buguagaoshu.community.model.AdminData;
import com.buguagaoshu.community.service.AdminDataService;
import com.buguagaoshu.community.util.NumberUtils;
import com.buguagaoshu.community.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-05 20:19
 */
@Service
public class AdminDataImpl implements AdminDataService {
    final
    AdminDataMapper adminDataMapper;

    @Autowired
    public AdminDataImpl(AdminDataMapper adminDataMapper) {
        this.adminDataMapper = adminDataMapper;
    }

    @Override
    public int insertAdminData(AdminData data) {
        return adminDataMapper.insertAdminData(data);
    }

    @Override
    public PaginationDto<AdminDataDTO> selectAdminData(String page, String size) {
        long allAdminData = adminDataMapper.getAllAdminDataCount();
        long[] param = NumberUtils.getPageAndSize(page, size, allAdminData);
        List<AdminData> adminDataList = adminDataMapper.selectAdmminData(param[0], param[1]);
        List<AdminDataDTO> adminDataDTOS = new ArrayList<>();
        for(AdminData adminData : adminDataList) {
            AdminDataDTO admin = new AdminDataDTO();
            BeanUtils.copyProperties(adminData, admin);
            admin.setTime(StringUtil.foematTime(adminData.getTime()));
            adminDataDTOS.add(admin);
        }

        PaginationDto<AdminDataDTO> paginationDto = new PaginationDto<>();
        paginationDto.setData(adminDataDTOS);
        paginationDto.setPagination(param[2], param[3], param[1]);
        return paginationDto;
    }

    @Override
    public AdminData getBestNetAdminData() {
        return adminDataMapper.getBestNetAdminData();
    }
}
