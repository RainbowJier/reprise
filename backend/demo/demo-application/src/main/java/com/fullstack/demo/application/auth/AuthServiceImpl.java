package com.fullstack.demo.application.auth;

import com.fullstack.common.base.enums.ResultCodeEnum;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.dto.auth.LoginReq;
import com.fullstack.demo.client.dto.auth.RegisterReq;
import com.fullstack.demo.client.dto.auth.TokenResp;
import com.fullstack.demo.client.service.AuthService;
import com.fullstack.demo.client.service.TokenProvider;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
import com.fullstack.demo.shared.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务实现：注册 / 登录 / 刷新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    public TokenResp register(RegisterReq req) {
        String username = req.getUsername();
        User exist = userGateway.findByUsername(username);
        if (exist != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "用户名已被注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : username);
        try {
            userGateway.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发重名由唯一索引兜底
            throw new BusinessException(ResultCodeEnum.CONFLICT, "用户名已被注册");
        }
        log.info("用户注册成功：{}", username);
        return tokenProvider.issueToken(user.getId(), username, user.getNickname());
    }

    @Override
    public TokenResp login(LoginReq req) {
        User user = userGateway.findByUsername(req.getUsername());
        // 用户不存在与密码错误使用同一文案，避免用户名枚举
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户名或密码错误");
        }
        log.info("用户登录成功：{}", user.getUsername());
        return tokenProvider.issueToken(user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public TokenResp refresh(String refreshToken) {
        // 无状态校验：验签 + 有效期 + 类型，不查库（设计已知局限：昵称取不到最新值）
        LoginUser login = tokenProvider.parseRefresh(refreshToken);
        return tokenProvider.issueToken(login.getUserId(), login.getUsername(), login.getUsername());
    }
}
