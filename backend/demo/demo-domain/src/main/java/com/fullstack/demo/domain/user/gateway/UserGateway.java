package com.fullstack.demo.domain.user.gateway;

import com.fullstack.demo.domain.user.User;

/**
 * 用户领域网关（仓储接口）。
 * <p>
 * 接口位于 domain，实现位于 demo-infrastructure（UserGatewayImpl，委托 UserMapper）；
 * application 层只依赖本接口，不触碰 MyBatis 细节。
 */
public interface UserGateway {

    /**
     * 按登录名查询（含逻辑删除过滤）。
     */
    User findByUsername(String username);

    /**
     * 按主键查询。
     */
    User findById(Long id);

    /**
     * 新增用户（回填雪花 ID）。
     */
    void insert(User user);
}
