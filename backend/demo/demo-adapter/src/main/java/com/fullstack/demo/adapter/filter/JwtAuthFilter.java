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
