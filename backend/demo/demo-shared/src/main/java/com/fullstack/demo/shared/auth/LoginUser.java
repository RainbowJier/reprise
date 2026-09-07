package com.fullstack.demo.shared.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录用户上下文载体（来自 access token 解析结果）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID（token sub）
     */
    private Long userId;

    /**
     * 登录名
     */
    private String username;
}
