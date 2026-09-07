package com.fullstack.demo.infrastructure.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fullstack.demo.domain.flash.FlashItem;
import com.fullstack.demo.domain.flash.gateway.FlashItemGateway;
import com.fullstack.demo.infrastructure.mapper.FlashItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀商品网关实现：委托 MyBatis-Plus Mapper。
 */
@Component
@RequiredArgsConstructor
public class FlashItemGatewayImpl implements FlashItemGateway {

    private final FlashItemMapper flashItemMapper;

    @Override
    public List<FlashItem> findAll() {
        return flashItemMapper.selectList(new LambdaQueryWrapper<FlashItem>().orderByAsc(FlashItem::getId));
    }

    @Override
    public FlashItem findById(Long id) {
        return flashItemMapper.selectById(id);
    }

    @Override
    public int deductStock(Long itemId) {
        return flashItemMapper.deductStock(itemId);
    }

    @Override
    public void resetStock(Long itemId) {
        flashItemMapper.resetStock(itemId);
    }
}
