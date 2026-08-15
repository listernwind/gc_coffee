package com.gccoffee.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数错误" : fe.getField() + " " + fe.getDefaultMessage();
        return Result.error(400, msg);
    }

    /** 请求体解析失败（JSON 格式/类型/日期错误） */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
        log.warn("参数解析失败: {}", e.getMessage());
        return Result.error(400, "请求参数格式错误");
    }

    /** 参数类型转换失败（如 date=abc、id=xyz） */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        return Result.error(400, "参数格式错误：" + e.getName());
    }

    /** 缺少必填参数 */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException e) {
        return Result.error(400, "缺少参数：" + e.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统开小差了，请稍后再试");
    }
}
