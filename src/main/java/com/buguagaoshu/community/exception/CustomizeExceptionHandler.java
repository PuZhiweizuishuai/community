package com.buguagaoshu.community.exception;

import com.buguagaoshu.community.dto.ResultDTO;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-25 17:25
 * 处理页面啊异常
 */
// Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [duplicate]
@ControllerAdvice
public class CustomizeExceptionHandler {
    @ExceptionHandler(value = CustomizeException.class)
    @ResponseBody
    public Object handle(Throwable e, Model model, HttpServletRequest request, HttpServletResponse response) {
        String contentType = request.getContentType();
        if ("application/json".equals(contentType)) {
            try {
                ResultDTO resultDTO;
                // 返回 JSON
                if (e instanceof CustomizeException) {
                    resultDTO = ResultDTO.errorOf((CustomizeException) e);
                } else {
                    resultDTO = ResultDTO.errorOf(CustomizeErrorCode.SYS_ERROR);
                }
                response.setContentType("application/json");
                response.setStatus(200);
                response.setCharacterEncoding("utf-8");
                return resultDTO;
            } catch (Exception ignore) {

            }
            return null;
        } else {
            // 错误页面跳转
            if (e instanceof CustomizeException) {
                model.addAttribute("message", e.getMessage());
            } else {
                model.addAttribute("message", CustomizeErrorCode.SYS_ERROR.getMessage());
            }
            return new ModelAndView("error");
        }

    }
}
