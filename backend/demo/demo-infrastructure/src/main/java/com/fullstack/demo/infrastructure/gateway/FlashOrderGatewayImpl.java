package com.fullstack.demo.infrastructure.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fullstack.demo.domain.flash.FlashOrder;
import com.fullstack.demo.domain.flash.gateway.FlashOrderGateway;
import com.fullstack.demo.infrastructure.mapper.FlashOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀订单网关实现：委托 MyBatis-Plus Mapper。
 */
@Component
@RequiredArgsConstructor
public class FlashOrderGatewayImpl implements FlashOrderGateway {

    private final FlashOrderMapper flashOrderMapper;

    @Override
    public void insert(FlashOrder order) {
        flashOrderMapper.insert(order);
    }

    @Override
    public FlashOrder findByItemAndUser(Long itemId, Long userId) {
        return flashOrderMapper.selectOne(new LambdaQueryWrapper<FlashOrder>()
                .eq(FlashOrder::getItemId, itemId)
                .eq(FlashOrder::getUserId, userId));
    }

    @Override
    public List<FlashOrder> findByUserIdOrderByCreateTimeDesc(Long userId) {
        return flashOrderMapper.selectList(new LambdaQueryWrapper<FlashOrder>()
                .eq(FlashOrder::getUserId, userId)
                .orderByDesc(FlashOrder::getCreateTime));
    }

    @Override
    public void deleteByItemId(Long itemId) {
        flashOrderMapper.deleteByItemId(itemId);
    }
}
