package com.fullstack.demo.adapter.controller;

import com.fullstack.common.base.entity.AjaxResult;
import com.fullstack.demo.client.dto.auth.LoginReq;
import com.fullstack.demo.client.dto.auth.RefreshReq;
import com.fullstack.demo.client.dto.auth.RegisterReq;
import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.client.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（白名单，无需 token）
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册（注册即登录，直接返回 token 对）。
     */
    @PostMapping("/register")
    public AjaxResult<TokenResp> register(@Valid @RequestBody RegisterReq req) {
        return AjaxResult.success(authService.register(req));
    }

    /**
     * 登录。
     */
    @PostMapping("/login")
    public AjaxResult<TokenResp> login(@Valid @RequestBody LoginReq req) {
        return AjaxResult.success(authService.login(req));
    }

    /**
     * 刷新 token（旋转双发）。
     */
    @PostMapping("/refresh")
    public AjaxResult<TokenResp> refresh(@Valid @RequestBody RefreshReq req) {
        return AjaxResult.success(authService.refresh(req.getRefreshToken()));
    }
}
