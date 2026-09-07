package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.user.UserInfoResp;

/**
 * 用户服务（实现位于 demo-application）
 */
public interface UserService {

    /**
     * 查询当前登录用户信息（取自 UserContextHolder，查库取最新资料）。
     */
    UserInfoResp getCurrentUser();
}
