package com.fullstack.demo.client.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录/注册/刷新成功返回的 token 对
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问令牌（短效，默认 30 分钟）
     */
    private String accessToken;

    /**
     * 刷新令牌（长效，默认 7 天）
     */
    private String refreshToken;

    /**
     * 令牌类型（固定 Bearer）
     */
    private String tokenType;

    /**
     * access token 有效期（秒）
     */
    private Long accessExpiresIn;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 登录名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;
}
