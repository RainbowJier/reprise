package com.fullstack.demo.infrastructure.auth;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 配置（app.jwt.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HS256 签名密钥（≥ 32 字节；生产环境必须替换并通过环境变量注入）
     */
    private String secret;

    /**
     * access token 有效期（秒），默认 30 分钟
     */
    private long accessExpireSeconds = 1800;

    /**
     * refresh token 有效期（秒），默认 7 天
     */
    private long refreshExpireSeconds = 604800;

    /**
     * 构造签名密钥（长度不足 32 字节时 hmacShaKeyFor 会抛异常，启动即暴露配置问题）。
     */
    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
