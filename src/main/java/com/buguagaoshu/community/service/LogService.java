package com.buguagaoshu.community.service;

import com.buguagaoshu.community.dto.LogDTO;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-07 21:11
 */
public interface LogService {
    /**
     * 加载日志资源
     * @param filename 日志名
     * @return 日志资源
     * */
    Resource loadLogForDown(String filename);


    /**
     * 获取日志列表
     * @return 日志列表
     * */
    List<LogDTO> getLogDtoList();

    /**
     * 删除日志数据
     * @param name 日志名
     * @return 结果
     * */
    boolean deleteLog(String name);
}
