package com.buguagaoshu.community.exception;

import com.buguagaoshu.community.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理：所有异常统一返回 ResultDTO JSON。
 * HTTP 风格错误码（400/401/403/404/409）映射到对应 HTTP 状态，
 * 业务错误码（1001~2020）保持 HTTP 200，由前端按 code 处理。
 */
@Slf4j
@RestControllerAdvice
public class CustomizeExceptionHandler {

    @ExceptionHandler(CustomizeException.class)
    public ResponseEntity<ResultDTO<Object>> handleCustomize(CustomizeException e) {
        return ResponseEntity.status(statusOf(e.getCode())).body(ResultDTO.errorOf(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultDTO<Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(CustomizeErrorCode.PARAM_ERROR.getMessage());
        return ResponseEntity.badRequest()
                .body(ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultDTO<Object>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultDTO<Object>> handleUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest()
                .body(ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "上传文件过大，请压缩后重试"));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResultDTO<Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResultDTO.errorOf(CustomizeErrorCode.PARAM_ERROR.getCode(), "请求方法不支持：" + e.getMethod()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ResultDTO<Object>> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResultDTO.errorOf(CustomizeErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultDTO<Object>> handleOther(Exception e) {
        log.error("未处理的接口异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR));
    }

    private HttpStatus statusOf(Integer code) {
        if (code == null) {
            return HttpStatus.OK;
        }
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401, 2003 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.OK;
        };
    }
}