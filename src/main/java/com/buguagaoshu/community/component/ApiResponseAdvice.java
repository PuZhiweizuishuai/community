package com.buguagaoshu.community.component;

import com.buguagaoshu.community.dto.ResultDTO;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应体增强：把 {@link ResultDTO#getStatus()} 同步到 HTTP 响应行状态。
 *
 * <p>这样成功与失败不仅体现在响应体 status 上，HTTP 状态码也保持一致，
 * 浏览器、网关与前端 fetch 都可以直接按 HTTP 状态判断结果。
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ResultDTO.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ResultDTO<?> result
                && result.getStatus() != null
                && response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.setStatusCode(HttpStatusCode.valueOf(result.getStatus()));
        }
        return body;
    }
}
