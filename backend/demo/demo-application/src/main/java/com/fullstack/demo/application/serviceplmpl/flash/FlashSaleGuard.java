package com.fullstack.demo.application.serviceplmpl.flash;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内存售罄标记（防超卖第一道防线）：每商品一个 AtomicBoolean，
 * 已确认售罄的商品 O(1) 直接拒绝，请求不再触达数据库。
 * <p>
 * 只做性能闸门，不承担正确性：标记丢失（多实例间不共享）只会多几次
 * 数据库原子更新（由 WHERE stock > 0 兜底），绝不会超卖。
 * 标记只进不出：本场景无库存回补；接入回补时须在回补处同步清除标记。
 */
@Component
public class FlashSaleGuard {

    private final ConcurrentHashMap<Long, AtomicBoolean> soldOutItems = new ConcurrentHashMap<>();

    /**
     * 商品是否已标记售罄。
     */
    public boolean isSoldOut(Long itemId) {
        AtomicBoolean flag = soldOutItems.get(itemId);
        return flag != null && flag.get();
    }

    /**
     * 标记售罄（库存原子扣减影响行数为 0 时调用）。
     */
    public void markSoldOut(Long itemId) {
        soldOutItems.computeIfAbsent(itemId, k -> new AtomicBoolean()).set(true);
    }

    /**
     * 清除售罄标记。正常交易流「只进不出」；仅演示重置等库存回补类运维动作调用，
     * 否则重置后所有请求会被第一道防线误杀（一律 6103）。
     */
    public void clearSoldOut(Long itemId) {
        soldOutItems.computeIfAbsent(itemId, k -> new AtomicBoolean()).set(false);
    }
}
