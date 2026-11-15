package com.buguagaoshu.community.exception;

import com.buguagaoshu.community.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：前后端分离后所有异常统一返回 JSON，不再跳转 Thymeleaf 错误页。
 */
@Slf4j
@RestControllerAdvice
public class CustomizeExceptionHandler {

    @ExceptionHandler(CustomizeException.class)
    public ResponseEntity<ResultDTO<Object>> handleCustomize(CustomizeException e) {
        HttpStatus status = (e.getCode() != null && (e.getCode() == 401 || e.getCode() == 2003))
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(ResultDTO.errorOf(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultDTO<Object>> handleOther(Exception e) {
        log.error("未处理的接口异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR));
    }
}
