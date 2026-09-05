package com.fullstack.common.base.entity;


import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fullstack.common.base.enums.ResultCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AjaxResult<T> {

    private long code;

    private String msg;

    private T data;

    public static <T> AjaxResult<T> success() {
        return new AjaxResult<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), null);
    }

    public static <T> AjaxResult<T> success(T data) {
        return new AjaxResult<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> AjaxResult<T> success(T data, String message) {
        return new AjaxResult<>(ResultCodeEnum.SUCCESS.getCode(), message, data);
    }

    public static <T> AjaxResult<T> failed(String message) {
        return new AjaxResult<>(ResultCodeEnum.FAILED.getCode(), message, null);
    }

    public boolean isSuccess() {
        return NumberUtil.equals(ResultCodeEnum.SUCCESS.getCode(), this.getCode());
    }

    public static <T> AjaxResult<T> failed() {
        return new AjaxResult<>(ResultCodeEnum.FAILED.getCode(), ResultCodeEnum.FAILED.getMessage(), null);
    }

    public static <T> AjaxResult<T> validateFailed() {
        return new AjaxResult<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), ResultCodeEnum.VALIDATE_FAILED.getMessage(), null);
    }

    public static <T> AjaxResult<T> validateFailed(String message) {
        return new AjaxResult<>(ResultCodeEnum.VALIDATE_FAILED.getCode(), message, null);
    }

    public static <T> AjaxResult<T> unauthorized() {
        return new AjaxResult<>(ResultCodeEnum.UNAUTHORIZED.getCode(), ResultCodeEnum.UNAUTHORIZED.getMessage(), null);
    }

    public static <T> AjaxResult<T> unauthorized(String message) {
        return new AjaxResult<>(ResultCodeEnum.UNAUTHORIZED.getCode(), message, null);
    }

    public static <T> AjaxResult<T> forbidden(String message) {
        return new AjaxResult<>(ResultCodeEnum.FORBIDDEN.getCode(), message, null);
    }

    public static <T> AjaxResult<T> badRequest(String message) {
        return new AjaxResult<>(ResultCodeEnum.BAD_REQUEST.getCode(), message, null);
    }

    public static <T> AjaxResult<T> buildCodeAndMessage(long code, String message) {
        return new AjaxResult<>(code, message, null);
    }

    public static <T> AjaxResult<T> buildResult(ResultCodeEnum resultCode) {
        return new AjaxResult<>(resultCode.getCode(), resultCode.getMessage(), null);
    }
}
