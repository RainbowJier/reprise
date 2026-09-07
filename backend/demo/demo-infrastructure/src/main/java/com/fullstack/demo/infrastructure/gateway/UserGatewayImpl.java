package com.fullstack.demo.infrastructure.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fullstack.demo.domain.user.User;
import com.fullstack.demo.domain.user.gateway.UserGateway;
import com.fullstack.demo.infrastructure.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户网关实现：委托 MyBatis-Plus Mapper。
 */
@Component
@RequiredArgsConstructor
public class UserGatewayImpl implements UserGateway {

    private final UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public void insert(User user) {
        userMapper.insert(user);
    }
}
