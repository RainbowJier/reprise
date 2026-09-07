package com.fullstack.common.webmvc.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fullstack.common.base.entity.AjaxResult;
import com.fullstack.common.base.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * MVC 全局异常处理器。
 * <p>
 * 相比 fjgtkj-2026 原实现，移除了 Sa-Token（NotLogin/NotPermission/NotRole）与
 * 验证码（CaptchaException）分支：demo 采用自研 JWT 过滤器（场景 01），
 * 认证失败在 JwtAuthFilter 内直接写响应，不经过本处理器。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理。
     */
    @ExceptionHandler(BusinessException.class)
    public AjaxResult<?> handleBusinessException(BusinessException e) {
        if (isClientError(e.getCode())) {
            log.warn("业务异常：{}", e.getMessage());
        } else {
            log.error("业务异常：{}", e.getMessage(), e);
        }
        try {
            return AjaxResult.buildCodeAndMessage(Long.parseLong(e.getCode()), e.getMessage());
        } catch (NumberFormatException ex) {
            return AjaxResult.failed(e.getMessage());
        }
    }

    /**
     * 参数校验异常处理。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = resolveFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("参数校验异常：{}", message);
        return AjaxResult.validateFailed(message);
    }

    /**
     * 绑定异常处理。
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult<?> handleBindException(BindException e) {
        String message = resolveFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("绑定异常：{}", message);
        return AjaxResult.validateFailed(message);
    }

    /**
     * 参数校验违例处理（@Validated 触发的参数级校验）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(e.getMessage());
        log.warn("参数校验违例：{}", message);
        return AjaxResult.validateFailed(message);
    }

    /**
     * 参数类型异常处理。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String parameterName = e.getName();
        String message = (parameterName == null || parameterName.isBlank())
                ? "参数类型不正确"
                : parameterName + "参数类型不正确";
        log.warn("参数类型异常：{}", message);
        return AjaxResult.validateFailed(message);
    }

    /**
     * 请求体不可读异常处理。
     *   - 非法 JSON
     *   - 请求体为空
     *   - 反序列化失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AjaxResult<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = resolveNotReadableMessage(e);
        log.warn("请求体格式异常：{}", message);
        return AjaxResult.validateFailed(message);
    }

    /**
     * Jackson 反序列化格式异常处理（如枚举值不匹配）。
     */
    @ExceptionHandler(InvalidFormatException.class)
    public AjaxResult<?> handleInvalidFormatException(InvalidFormatException e) {
        String message = "参数值不正确";
        if (e.getTargetType().isEnum()) {
            message = "参数值必须是枚举类型之一";
        }
        log.warn("反序列化异常：{}", message);
        return AjaxResult.validateFailed(message);
    }

    /**
     * 系统异常处理。
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult<?> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return AjaxResult.failed("系统异常，请联系管理员");
    }

    private boolean isClientError(String code) {
        try {
            long value = Long.parseLong(code);
            return value >= 400 && value < 500;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String resolveFieldErrorMessage(FieldError fieldError) {
        return fieldError == null ? "参数检验失败" : fieldError.getDefaultMessage();
    }

    private String resolveNotReadableMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String detail = cause == null ? exception.getMessage() : cause.getMessage();
        if (detail == null || detail.isBlank()) {
            return "请求体格式错误";
        }
        if (detail.contains("Required request body is missing")) {
            return "请求体不能为空";
        }
        return "请求体格式错误";
    }
}
