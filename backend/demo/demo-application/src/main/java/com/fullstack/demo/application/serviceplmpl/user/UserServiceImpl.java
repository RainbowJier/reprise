package com.fullstack.demo.application.serviceplmpl.user;

import com.fullstack.common.base.enums.ResultCodeEnum;
import com.fullstack.common.base.exception.BusinessException;
import com.fullstack.demo.client.dto.user.UserInfoResp;
import com.fullstack.demo.client.service.UserService;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
import com.fullstack.demo.shared.auth.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserGateway userGateway;

    @Override
    public UserInfoResp getCurrentUser() {
        // 过滤器保证上下文存在，此处兜底防御
        if (UserContextHolder.get() == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        User user = userGateway.findById(UserContextHolder.get().getUserId());
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在或已注销");
        }
        return new UserInfoResp(user.getId(), user.getUsername(), user.getNickname(), user.getCreateTime());
    }
}
