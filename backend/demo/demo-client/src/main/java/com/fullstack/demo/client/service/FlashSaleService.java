package com.fullstack.demo.client.service;

import com.fullstack.demo.client.dto.flash.FlashItemResp;
import com.fullstack.demo.client.dto.flash.FlashOrderResp;

import java.util.List;

/**
 * 秒杀服务端口（场景 03：秒杀抢购）。
 * <p>
 * 当前用户取自 UserContextHolder（/flash/* 由 JwtAuthFilter 注入上下文），
 * 实现位于 demo-application。
 */
public interface FlashSaleService {

    /**
     * 秒杀商品列表：实时库存、服务端推导的活动状态、当前用户的已抢标记。
     */
    List<FlashItemResp> listItems();

    /**
     * 抢购下单。失败抛 BusinessException：
     * 404 商品不存在 / 6101 未开始 / 6102 已结束 / 6103 已售罄 / 6104 重复抢购。
     */
    FlashOrderResp seckill(Long itemId);

    /**
     * 当前用户的抢购订单（时间倒序）。
     */
    List<FlashOrderResp> myOrders();

    /**
     * 演示专用：重置商品库存回满、清空其订单并清除售罄标记，
     * 使前端并发演示可反复运行。仅进行中的活动可重置（否则 417）；
     * 生产环境此类运维动作必须加管理权限并审计。
     */
    void resetDemoItem(Long itemId);
}
