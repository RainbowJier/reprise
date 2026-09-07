package com.fullstack.demo.adapter.controller;

import com.fullstack.common.base.entity.AjaxResult;
import com.fullstack.demo.client.dto.user.UserInfoResp;
import com.fullstack.demo.client.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口（受 JwtAuthFilter 保护）
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 当前登录用户信息。
     */
    @GetMapping("/me")
    public AjaxResult<UserInfoResp> me() {
        return AjaxResult.success(userService.getCurrentUser());
    }
}
