package com.fullstack.common.base.exception;

import com.fullstack.common.base.enums.ResultCodeEnum;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误信息
     */
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = "500";
        this.message = message;
    }

    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.code = String.valueOf(resultCode.getCode());
        this.message = resultCode.getMessage();
    }

    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.code = String.valueOf(resultCode.getCode());
        this.message = message;
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = "500";
        this.message = message;
    }

    public BusinessException(ResultCodeEnum resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = String.valueOf(resultCode.getCode());
        this.message = message;
    }
}
