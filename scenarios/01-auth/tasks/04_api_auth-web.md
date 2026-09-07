---
id: auth-jwt_04_api_auth_web
name: "Web 层与认证过滤器（Controller + JwtAuthFilter）"
type: api
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_03_service_auth_core]
profiles: [backend, api]
files:
  - backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/AuthController.java
  - backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/UserController.java
  - backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/filter/JwtAuthFilter.java
  - backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/config/AuthFilterConfig.java
---

# Web 层与认证过滤器（Controller + JwtAuthFilter）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `api` |
| 已启用 profile | backend / api |
| 架构边界 | demo-adapter（Controller 全在此层；过滤器与其注册配置同层，common-webmvc 传递 spring-web） |
| 结构分析 | 只读降级分析：过滤器异常不经过 `@RestControllerAdvice`，401 响应由过滤器自写 JSON（与 GlobalExceptionHandler 输出同构） |
| 完成条件 | `mvn test` 通过；`/auth/**`、`/health` 不经过过滤逻辑，`/user/*` 需有效 access token |

## 需求与验收

- 用户目标：暴露 4 个契约接口并对受保护路径做 token 鉴权。
- 包含：AuthController（register/login/refresh）、UserController（me）、JwtAuthFilter（OPTIONS 放行、解析注入 ThreadLocal、失败写 401）、FilterRegistrationBean 只拦 `/user/*`。
- 不包含：测试用例（任务 05）、白名单 `/auth` 的显式排除逻辑（用 URL pattern 天然排除）。
- 验收：无 token 访问 `/user/me` 返回 HTTP 200 + `code=401` JSON；有效 token 返回用户信息；`/api/health` 行为不变。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 契约见设计文档「六、接口设计」；参数校验失败由 GlobalExceptionHandler 统一返回 417 |
| 数据/Schema | 不适用（经服务层） |
| 集成 | ObjectMapper 用 Spring Boot 自动配置实例，保证序列化行为与响应体一致 |
| 安全与质量 | 401 打 warn 日志（不含 token 原文）；ThreadLocal 在 finally 清理防泄漏 |
| 依赖 | 03（服务实现与 TokenProvider Bean） |

## 实现步骤

1. 新建 `AuthController`、`UserController`（`@Valid @RequestBody` 触发校验）；
2. 新建 `JwtAuthFilter`（非 `@Component`，避免 Spring 对 Filter 的全路径自动注册）；
3. 新建 `AuthFilterConfig` 用 `FilterRegistrationBean` 注册并限定 `/user/*`；
4. `mvn test` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/AuthController.java` | adapter | 新增 | /auth 三个接口 |
| `backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/UserController.java` | adapter | 新增 | /user/me |
| `backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/filter/JwtAuthFilter.java` | adapter | 新增 | 鉴权过滤器 |
| `backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/config/AuthFilterConfig.java` | adapter | 新增 | 过滤器注册 |

## 完整代码（供手动敲写）

### backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/AuthController.java（新增）

```java
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
```

### backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/UserController.java（新增）

```java
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
```

### backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/filter/JwtAuthFilter.java（新增）

```java
package com.fullstack.demo.adapter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullstack.common.base.entity.AjaxResult;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.service.TokenProvider;
import com.fullstack.demo.shared.auth.AuthConstants;
import com.fullstack.demo.shared.auth.LoginUser;
import com.fullstack.demo.shared.auth.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 鉴权过滤器：解析 access token 并注入用户上下文。
 * <p>
 * 不注册为 @Component（避免 Spring 全路径自动注册），由 AuthFilterConfig
 * 以 FilterRegistrationBean 限定只拦截 /user/*；/auth/**、/health 天然不经过本过滤器。
 * 过滤器内异常不经过 @RestControllerAdvice，401 响应在此自写，
 * 与 GlobalExceptionHandler 输出同构（HTTP 200 + body code=401）。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // CORS 预检请求不携带 token，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, "未登录，请先登录");
            return;
        }
        try {
            LoginUser loginUser = tokenProvider.parseAccess(token);
            UserContextHolder.set(loginUser);
            try {
                chain.doFilter(request, response);
            } finally {
                UserContextHolder.clear();
            }
        } catch (BusinessException e) {
            writeUnauthorized(response, e.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AuthConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(AuthConstants.TOKEN_PREFIX)) {
            return bearer.substring(AuthConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        log.warn("认证失败：{}", message);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(AjaxResult.unauthorized(message)));
    }
}
```

### backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/config/AuthFilterConfig.java（新增）

```java
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
 * JWT 过滤器注册：只拦截 /user/*。
 * <p>
 * 后续场景新增受保护路径时在此扩展 patterns 或改为全路径 + 白名单。
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
        registration.addUrlPatterns("/user/*");
        // 放在最低优先级，确保 CORS 等更外层处理先行
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
```

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 全部通过 |
| manual | 启动后 `curl http://localhost:8080/api/user/me` | HTTP 200 + `{"code":401,...}` |
| manual | `curl http://localhost:8080/api/health` | 行为不变（不经过过滤器） |

## 风险与阻塞

- 风险：过滤器误标 `@Component` 会导致被 Spring 自动注册到 `/*`，白名单接口被误拦——代码已按非组件设计，注册唯一入口为 `AuthFilterConfig`。
- 阻塞：无。
- 执行记录：2026-09-05 落盘 4 个文件，无偏差；`mvn test` 11/11 通过（含任务 05 用例对过滤器 401 分支的黑盒验证）。
