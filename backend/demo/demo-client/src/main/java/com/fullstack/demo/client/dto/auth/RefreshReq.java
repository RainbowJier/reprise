package com.fullstack.demo.client.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新 token 请求
 */
@Data
public class RefreshReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
