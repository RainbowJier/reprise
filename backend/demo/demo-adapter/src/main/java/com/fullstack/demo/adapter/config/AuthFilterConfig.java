package com.fullstack.demo.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullstack.demo.adapter.filter.JwtAuthFilter;
import com.fullstack.demo.client.service.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * JWT 过滤器注册：拦截 /user/* 与 /flash/*（场景 03 秒杀域受保护路径）。
 * <p>
 * /auth/**、/health 不在 patterns 内，天然白名单；
 * 后续场景新增受保护路径时继续在此扩展 patterns 或改为全路径 + 白名单。
 */
@Configuration
@RequiredArgsConstructor
public class AuthFilterConfig {

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration() {
        FilterRegistrationBean<JwtAuthFilter> registration =
                new FilterRegistrationBean<>(new JwtAuthFilter(tokenProvider, objectMapper));
        registration.addUrlPatterns("/user/*", "/flash/*");
        // 放在最低优先级，确保 CORS 等更外层处理先行
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
