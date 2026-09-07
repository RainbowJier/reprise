package com.fullstack.demo.shared.auth;

/**
 * 认证相关常量
 */
public final class AuthConstants {

    /**
     * token 请求头
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * token 类型 claim 名
     */
    public static final String CLAIM_TYPE = "type";

    /**
     * 用户名 claim 名
     */
    public static final String CLAIM_USERNAME = "username";

    /**
     * access token 类型
     */
    public static final String TYPE_ACCESS = "access";

    /**
     * refresh token 类型
     */
    public static final String TYPE_REFRESH = "refresh";

    private AuthConstants() {
    }
}
