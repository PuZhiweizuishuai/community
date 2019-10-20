package com.buguagaoshu.community.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-20 11:57
 */
public class VditorDTO {
    private String msg;
    private Integer code;
    private Map<String, Object> data;

    public VditorDTO (String msg, Integer code,
                               String[] errFiles, Map<String, Object> succMap) {
        this.msg = msg;
        this.code = code;
        data = new HashMap<>(4);
        Map<String, Object> file = new HashMap<>(2);
        file.put("errFiles", errFiles);
        file.put("succMap", succMap);
        data.put("data", file);
    }
}
