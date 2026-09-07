---
id: flash-sale_04_api_flash_web
name: "秒杀 Web 接口（Controller + 鉴权过滤器扩展 /flash/*）"
type: api
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_03_service_seckill_core]
profiles: [backend, api]
files:
  - backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/FlashSaleController.java
  - backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/config/AuthFilterConfig.java
---

# 秒杀 Web 接口（Controller + 鉴权过滤器扩展 /flash/*）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `api` |
| 已启用 profile | backend / api |
| 架构边界 | demo-adapter（Controller + 过滤器注册配置，全部 Controller 落位 adapter） |
| 结构分析 | AuthFilterConfig 的 javadoc 明确预留扩展点（「后续场景新增受保护路径时在此扩展 patterns」）；/flash/* 为新路径，无既有调用方，影响面=新增拦截 |
| 完成条件 | `mvn test` 通过；/flash/** 无 token 返回 code=401 |

## 需求与验收

- 用户目标：三个 HTTP 接口（列表/抢购/我的订单）；/flash/* 纳入 JWT 鉴权。
- 包含：FlashSaleController、AuthFilterConfig 扩展 urlPatterns。
- 验收：`mvn test` 通过；携带 token 后接口可用（功能验证在任务 05 展开）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 认证边界 | `addUrlPatterns("/user/*", "/flash/*")`：/flash 整域受保护（列表需标记「我已抢」、下单需身份）；/auth、/health 保持白名单 |
| 契约一致性 | 路径与任务 02/06 冻结的契约一致：GET /flash/items、POST /flash/items/{itemId}/seckill、GET /flash/orders/mine |
| 依赖 | 03（服务实现）；被 05（集成测试）、06（前端联调）依赖 |

## 实现步骤

1. adapter 新建 FlashSaleController；
2. 修改 AuthFilterConfig 的 urlPatterns 与注释；
3. `mvn test` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-adapter/.../controller/FlashSaleController.java` | adapter | 新增 | 三个接口 |
| `backend/demo/demo-adapter/.../config/AuthFilterConfig.java` | adapter | 修改 | patterns 增加 /flash/* |

## 完整代码（供手动敲写）

### backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/controller/FlashSaleController.java（新增）

```java
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
}
```

### backend/demo/demo-adapter/src/main/java/com/fullstack/demo/adapter/config/AuthFilterConfig.java（修改）

```java
/**
 * JWT 过滤器注册：拦截 /user/* 与 /flash/*（场景 03 秒杀域受保护路径）。
 * <p>
 * /auth/**、/health 不在 patterns 内，天然白名单；
 * 后续场景新增受保护路径时继续在此扩展 patterns 或改为全路径 + 白名单。
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
        registration.addUrlPatterns("/user/*", "/flash/*");
        // 放在最低优先级，确保 CORS 等更外层处理先行
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
```

修改点说明：仅两处——类 javadoc 首句改为「拦截 /user/* 与 /flash/*（场景 03 秒杀域受保护路径）」；`addUrlPatterns("/user/*")` 改为 `addUrlPatterns("/user/*", "/flash/*")`。其余不动。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 全部通过（场景 01 用例不受影响——/user/* 拦截行为不变） |
| manual | `curl -s http://localhost:8080/api/flash/items` | `{"code":401,...}`（未携带 token） |

## 风险与阻塞

- 风险：/flash/** 从「无保护 404」变为「受保护 401」，新路径无既有调用方，无兼容性影响。
- 阻塞：无。
- 执行记录：2026-09-07 Controller 落盘；AuthFilterConfig patterns 扩展为 /user/* + /flash/*；`mvn test` 通过，未带 token 访问 /flash/items 返回 code=401（Order(1) 用例验证）。
