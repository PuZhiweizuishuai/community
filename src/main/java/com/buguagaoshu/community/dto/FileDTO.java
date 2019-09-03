package com.buguagaoshu.community.dto;

import lombok.Data;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 18:44
 */
@Data
public class FileDTO {
    private Integer success;

    private String message;

    private String url;

    public FileDTO(Integer success, String message, String url) {
        this.success = success;
        this.message = message;
        this.url = url;
    }

    public FileDTO() {

    }
}
