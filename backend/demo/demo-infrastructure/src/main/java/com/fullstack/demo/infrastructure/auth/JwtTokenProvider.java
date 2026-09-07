package com.fullstack.demo.infrastructure.auth;

import com.fullstack.common.base.enums.ResultCodeEnum;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.client.service.TokenProvider;
import com.fullstack.demo.shared.auth.AuthConstants;
import com.fullstack.demo.shared.auth.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * jjwt 实现的 token 签发与解析（HS256）。
 * <p>
 * 实现 client 层 TokenProvider 端口，由组件扫描装配，application 层经接口调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

    private final JwtProperties jwtProperties;

    @Override
    public TokenResp issueToken(Long userId, String username, String nickname) {
        long now = System.currentTimeMillis();
        String accessToken = buildToken(userId, username, AuthConstants.TYPE_ACCESS,
                now, jwtProperties.getAccessExpireSeconds());
        String refreshToken = buildToken(userId, username, AuthConstants.TYPE_REFRESH,
                now, jwtProperties.getRefreshExpireSeconds());
        return TokenResp.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessExpiresIn(jwtProperties.getAccessExpireSeconds())
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .build();
    }

    @Override
    public LoginUser parseAccess(String token) {
        return parse(token, AuthConstants.TYPE_ACCESS);
    }

    @Override
    public LoginUser parseRefresh(String token) {
        return parse(token, AuthConstants.TYPE_REFRESH);
    }

    private String buildToken(Long userId, String username, String type, long now, long expireSeconds) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(AuthConstants.CLAIM_USERNAME, username)
                .claim(AuthConstants.CLAIM_TYPE, type)
                // jti 唯一 ID：同一秒内重复签发也保证 token 字节级不同（亦为未来黑名单留扩展点）
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireSeconds * 1000))
                .signWith(jwtProperties.getSecretKey())
                .compact();
    }

    private LoginUser parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtProperties.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String type = claims.get(AuthConstants.CLAIM_TYPE, String.class);
            if (!expectedType.equals(type)) {
                throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "token 类型不正确");
            }
            return new LoginUser(Long.valueOf(claims.getSubject()),
                    claims.get(AuthConstants.CLAIM_USERNAME, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            // 只记录异常摘要，不落 token 原文
            log.warn("JWT 解析失败：{}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
    }
}
