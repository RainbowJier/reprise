package com.fullstack.demo.adapter.controller;

import com.fullstack.common.base.entity.AjaxResult;
import com.fullstack.demo.client.dto.flash.FlashItemResp;
import com.fullstack.demo.client.dto.flash.FlashOrderResp;
import com.fullstack.demo.client.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀接口（场景 03）。
 * <p>
 * /flash/* 由 JwtAuthFilter 拦截（见 AuthFilterConfig），当前用户经
 * UserContextHolder 注入；业务失败（未开始/已结束/售罄/重复抢购）由
 * GlobalExceptionHandler 统一转换为 AjaxResult。
 */
@RestController
@RequestMapping("/flash")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    /**
     * 秒杀商品列表（实时库存/服务端状态/我已抢标记）。
     */
    @GetMapping("/items")
    public AjaxResult<List<FlashItemResp>> listItems() {
        return AjaxResult.success(flashSaleService.listItems());
    }

    /**
     * 抢购下单：成功返回订单，失败返回 61xx 业务码。
     */
    @PostMapping("/items/{itemId}/seckill")
    public AjaxResult<FlashOrderResp> seckill(@PathVariable Long itemId) {
        return AjaxResult.success(flashSaleService.seckill(itemId));
    }

    /**
     * 当前用户的抢购订单（时间倒序）。
     */
    @GetMapping("/orders/mine")
    public AjaxResult<List<FlashOrderResp>> myOrders() {
        return AjaxResult.success(flashSaleService.myOrders());
    }

    /**
     * 演示专用：重置商品库存与订单，使前端并发演示可反复运行。
     * 生产环境此类运维动作必须加管理权限并审计，绝不对普通用户开放。
     */
    @PostMapping("/demo/reset/{itemId}")
    public AjaxResult<Void> resetDemo(@PathVariable Long itemId) {
        flashSaleService.resetDemoItem(itemId);
        return AjaxResult.success();
    }
}
