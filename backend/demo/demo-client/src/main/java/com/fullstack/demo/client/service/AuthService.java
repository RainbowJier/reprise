package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.auth.LoginReq;
import com.fullstack.demo.client.dto.auth.RegisterReq;
import com.fullstack.demo.client.dto.auth.TokenResp;

/**
 * 认证服务（实现位于 demo-application）
 */
public interface AuthService {

    /**
     * 注册并直接返回登录态（注册即登录）。
     */
    TokenResp register(RegisterReq req);

    /**
     * 用户名密码登录。
     */
    TokenResp login(LoginReq req);

    /**
     * 使用 refresh token 换取新 token 对（旋转双发）。
     */
    TokenResp refresh(String refreshToken);
}
