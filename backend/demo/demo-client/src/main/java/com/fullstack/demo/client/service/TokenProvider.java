package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.shared.auth.LoginUser;

/**
 * token 签发与解析端口。
 * <p>
 * 接口位于 client 层供 application 依赖倒置调用；
 * jjwt 实现位于 demo-infrastructure（JwtTokenProvider），由 starter 装配。
 */
public interface TokenProvider {

    /**
     * 签发 access + refresh 双 token（旋转双发）。
     *
     * @param userId   用户 ID
     * @param username 登录名
     * @param nickname 昵称
     * @return token 对
     */
    TokenResp issueToken(Long userId, String username, String nickname);

    /**
     * 解析并校验 access token（验签 + 有效期 + 类型）。
     *
     * @param token access token 原文
     * @return 登录用户上下文
     * @throws com.fullstack.common.base.exception.BusinessException 校验失败（code=401）
     */
    LoginUser parseAccess(String token);

    /**
     * 解析并校验 refresh token（验签 + 有效期 + 类型）。
     *
     * @param token refresh token 原文
     * @return 登录用户上下文
     * @throws com.fullstack.common.base.exception.BusinessException 校验失败（code=401）
     */
    LoginUser parseRefresh(String token);
}
