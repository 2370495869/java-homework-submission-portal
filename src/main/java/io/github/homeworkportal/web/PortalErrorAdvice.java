package io.github.homeworkportal.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import java.util.NoSuchElementException;

@ControllerAdvice(annotations = Controller.class)
public class PortalErrorAdvice {
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView forbidden(AccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "没有访问权限。");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ModelAndView notFound(NoSuchElementException exception) {
        return error(HttpStatus.NOT_FOUND, "请求的内容不存在。");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView tooLarge(MaxUploadSizeExceededException exception) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "上传请求超过 10 MiB 文件限制。");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView badRequest(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "请求内容无效。" : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, message);
    }

    private static ModelAndView error(HttpStatus status, String message) {
        ModelAndView view = new ModelAndView("error");
        view.setStatus(status);
        view.addObject("status", status.value());
        view.addObject("message", message);
        return view;
    }
}
