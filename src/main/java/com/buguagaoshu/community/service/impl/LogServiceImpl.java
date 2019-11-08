package com.buguagaoshu.community.service.impl;

import com.buguagaoshu.community.dto.LogDTO;
import com.buguagaoshu.community.service.LogService;
import com.buguagaoshu.community.util.FileUtil;
import com.buguagaoshu.community.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-07 21:12
 */
@Service
@Slf4j
public class LogServiceImpl implements LogService {
    /**
     * 日志文件目录
     */
    @Value("${File.LOG.PATH}")
    private String LOG_PATH;

    @Override
    public Resource loadLogForDown(String filename) {
        String pathStr = LOG_PATH + "/";
        Path file = Paths.get(pathStr);
        try {
            Path name = file.resolve(filename);
            Resource resource = new UrlResource(name.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            log.error("资源加载异常，代码位置LogServiceImpl第45行 {}", e.getMessage());
        }
        return null;
    }

    @Override
    public List<LogDTO> getLogDtoList() {
        String pathStr = LOG_PATH + "/";
        try {
            Path files = Paths.get(pathStr);
            List<LogDTO> logDtoList = new ArrayList<>();
            Files.walk(files, 1)
                    .filter(file -> FileUtil.checkLogFileType(file.getFileName().toString()))
                    .forEach(file -> {
                        LogDTO logDTO = new LogDTO();
                        logDTO.setTitle(file.getFileName().toString());
                        logDTO.setUrl(file.getFileName().toString());
                        try {
                            logDTO.setTime(StringUtil.foematTime(Files.getLastModifiedTime(file).toMillis()));
                        } catch (IOException e) {
                            logDTO.setTime("未知时间");
                        }
                        logDTO.setShow(FileUtil.checkLogShow(file.getFileName().toString()));
                        logDtoList.add(logDTO);
                    });
            return logDtoList;
        } catch (Exception e) {
            log.error("文件遍历错误：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteLog(String name) {
        String pathStr = LOG_PATH + "/";
        try {
            Path files = Paths.get(pathStr).resolve(name);
            Files.delete(files);
            return true;
        } catch (IOException e) {
            log.error("文件删除错误：{}", e.getMessage());
        }
        return false;
    }
}
