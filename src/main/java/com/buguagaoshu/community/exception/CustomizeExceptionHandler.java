package com.buguagaoshu.community.exception;

import com.buguagaoshu.community.dto.ResultDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理：所有异常统一返回 {@code { status, message, timestamp, data }}。
 * 响应体 status 与 HTTP 响应行状态保持一致，前端只需按 status 判断成功或失败。
 *
 * <p>参数校验统一走 jakarta.validation：请求体 / 表单对象校验失败抛 {@link BindException}，
 * 方法参数（@RequestParam / @PathVariable）校验失败抛 {@link HandlerMethodValidationException}，
 * 由本类统一转换为 400 + 首条校验提示，Controller 中不再手工判空与格式判断。
 */
@Slf4j
@RestControllerAdvice
public class CustomizeExceptionHandler {

    @ExceptionHandler(CustomizeException.class)
    public ResponseEntity<ResultDTO<Object>> handleCustomize(CustomizeException e) {
        return ResponseEntity.status(e.getStatus()).body(ResultDTO.errorOf(e));
    }

    /** 请求体（@RequestBody）与表单对象（@ModelAttribute）的 Bean Validation 校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResultDTO<Object>> handleBind(BindException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(CustomizeErrorCode.PARAM_ERROR.getMessage());
        return badRequest(message);
    }

    /** 方法参数（@RequestParam / @PathVariable）上的约束校验失败 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ResultDTO<Object>> handleMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(CustomizeErrorCode.PARAM_ERROR.getMessage());
        return badRequest(message);
    }

    /** 非 Controller 层（如 @Validated 代理）触发的约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultDTO<Object>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(CustomizeErrorCode.PARAM_ERROR.getMessage());
        return badRequest(message);
    }

    /** 参数类型不匹配（如 page=abc）或缺少必填参数 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ResultDTO<Object>> handleParamMismatch(Exception e) {
        return badRequest(CustomizeErrorCode.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultDTO<Object>> handleUnreadable(HttpMessageNotReadableException e) {
        return badRequest(CustomizeErrorCode.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultDTO<Object>> handleUploadSize(MaxUploadSizeExceededException e) {
        return badRequest("上传文件过大，请压缩后重试");
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResultDTO<Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResultDTO.errorOf(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持：" + e.getMethod()));
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

    private ResponseEntity<ResultDTO<Object>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ResultDTO.errorOf(HttpStatus.BAD_REQUEST.value(), message));
    }
}
