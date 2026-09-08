---
id: flash-sale_01_data_model_flash_tables
name: "秒杀数据模型（flash_items / flash_orders 双表 + 原子扣减 Mapper）"
type: data_model
subtype: null
status: in_progress
blocked_reason: null
depends: []
profiles: [backend, database]
files:
  - backend/demo/demo-starter/src/main/resources/schema.sql
  - backend/demo/demo-starter/src/main/resources/data.sql
  - scenarios/03-flash-sale/sql/postgresql.sql
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/FlashItem.java
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/FlashOrder.java
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/gateway/FlashItemGateway.java
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/gateway/FlashOrderGateway.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/FlashItemMapper.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/FlashOrderMapper.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/FlashItemGatewayImpl.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/FlashOrderGatewayImpl.java
---

# 秒杀数据模型（flash_items / flash_orders 双表 + 原子扣减 Mapper）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `data_model` |
| 已启用 profile | backend / database |
| 架构边界 | demo-domain（实体 + Gateway 端口）；demo-infrastructure（Mapper + Gateway 实现）；demo-starter（schema.sql / data.sql 种子）；scenarios/03-flash-sale/sql（PostgreSQL 手工脚本） |
| 结构分析 | 只读降级分析：沿用场景 01 模式——实体继承 BaseIdEntity（雪花 ID + 审计字段），Mapper 落位 infrastructure.mapper（@MapperScan），Gateway 端口在 domain、实现在 infrastructure |
| 完成条件 | `mvn test` 通过（既有 11 个用例不受影响，schema/data.sql 为幂等追加） |

## 需求与验收

- 用户目标：秒杀商品与订单两张表；订单表带 `uk(item_id, user_id)` 唯一索引（限购兜底）；商品表 `total_stock`/`stock` 双字段（对账基准）；金额一律 BIGINT 存分。
- 包含：H2 建表与种子数据（四档状态各一件：进行中×2、未开始、已结束）、PostgreSQL 手工脚本同步、实体、Mapper（含 `deductStock` 原子条件更新）、Gateway 端口与实现。
- 不包含：服务编排（任务 03）、接口（任务 04）。
- 验收：`mvn test` 通过；启动后（含测试上下文）`flash_items` 有 4 行种子。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 数据变更策略 | 项目既有策略：schema.sql / data.sql 由 spring.sql.init 在 H2 启动时自动执行（幂等 CREATE TABLE IF NOT EXISTS）；PostgreSQL 不自动执行，手工脚本放场景目录 |
| 防超卖核心 | `deductStock` 由 `FlashItemMapper.xml` 承载原生 SQL：`UPDATE flash_items SET stock = stock - 1 WHERE id = ? AND stock > 0 AND deleted = 0`——判断与扣减同语句原子完成，只认影响行数 |
| 种子时间 | 相对 `CURRENT_TIMESTAMP` 生成（-10min 进行中、+1day 未开始、-1day 已结束），保证任意时刻启动都有四档状态可演示 |
| 方言边界 | data.sql 仅 H2 方言（`CURRENT_TIMESTAMP - INTERVAL '10' MINUTE`）；PostgreSQL 用 `now() - interval '10 minutes'` 写在独立脚本 |
| 依赖 | 无前置依赖；后续任务 02/03 依赖本任务的实体与 Gateway |

## 实现步骤

1. schema.sql 追加两张表（含唯一索引）；
2. data.sql 追加 4 行种子商品；
3. domain 新建 flash 包：两个实体 + gateway 两个端口；
4. infrastructure 新建两个 Mapper 与同包名 XML（FlashItemMapper.xml 含 deductStock/resetStock），再实现两个 Gateway；
5. 新建 scenarios/03-flash-sale/sql/postgresql.sql（核对场景 01 脚本后追加秒杀段）；
6. `mvn test` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/demo/demo-starter/src/main/resources/schema.sql` | starter | 修改 | 追加两表 DDL |
| `backend/demo/demo-starter/src/main/resources/data.sql` | starter | 修改 | 追加种子商品 |
| `scenarios/03-flash-sale/sql/postgresql.sql` | 场景目录 | 新增 | PostgreSQL 手工脚本 |
| `backend/demo/demo-domain/.../domain/flash/FlashItem.java` | domain | 新增 | 秒杀商品实体 |
| `backend/demo/demo-domain/.../domain/flash/FlashOrder.java` | domain | 新增 | 秒杀订单实体 |
| `backend/demo/demo-domain/.../domain/flash/gateway/FlashItemGateway.java` | domain | 新增 | 商品网关端口（含原子扣减） |
| `backend/demo/demo-domain/.../domain/flash/gateway/FlashOrderGateway.java` | domain | 新增 | 订单网关端口 |
| `backend/demo/demo-infrastructure/.../mapper/FlashItemMapper.java` | infrastructure | 新增 | 仅声明 deductStock/resetStock 方法 |
| `backend/demo/demo-infrastructure/.../mapper/FlashOrderMapper.java` | infrastructure | 新增 | 空扩展 Mapper |
| `backend/demo/demo-infrastructure/.../gateway/FlashItemGatewayImpl.java` | infrastructure | 新增 | 商品网关实现 |
| `backend/demo/demo-infrastructure/.../gateway/FlashOrderGatewayImpl.java` | infrastructure | 新增 | 订单网关实现 |
| `backend/demo/demo-infrastructure/src/main/resources/mapper/FlashItemMapper.xml` | infrastructure | 新增 | deductStock/resetStock XML SQL |

## 完整代码（供手动敲写）

### backend/demo/demo-starter/src/main/resources/schema.sql（修改，文件末尾追加）

```sql
-- 场景 03：秒杀抢购 flash_items / flash_orders
-- PostgreSQL 不自动执行，手工脚本见 scenarios/03-flash-sale/sql/postgresql.sql
-- 金额单位为分（BIGINT）；total_stock 为初始库存（对账基准），stock 为剩余库存
CREATE TABLE IF NOT EXISTS flash_items (
    id             BIGINT       NOT NULL,
    name           VARCHAR(100) NOT NULL,
    price          BIGINT       NOT NULL,
    original_price BIGINT       NOT NULL,
    total_stock    INT          NOT NULL,
    stock          INT          NOT NULL,
    start_time     TIMESTAMP    NOT NULL,
    end_time       TIMESTAMP    NOT NULL,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    create_by      BIGINT,
    update_by      BIGINT,
    deleted        INT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_flash_items PRIMARY KEY (id)
);

-- 唯一索引 (item_id, user_id)：每人限购一件的物理保证（防超卖第三道防线）
CREATE TABLE IF NOT EXISTS flash_orders (
    id          BIGINT       NOT NULL,
    item_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    item_name   VARCHAR(100) NOT NULL,
    price       BIGINT       NOT NULL,
    status      INT DEFAULT 0 NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     INT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_flash_orders PRIMARY KEY (id),
    CONSTRAINT uk_flash_orders_item_user UNIQUE (item_id, user_id)
);
```

### backend/demo/demo-starter/src/main/resources/data.sql（修改，文件末尾追加）

```sql
-- 场景 03 种子数据：四档状态各一件（进行中×2 / 未开始 / 已结束），时间相对启动时刻生成
-- H2 方言；PostgreSQL 手工脚本见 scenarios/03-flash-sale/sql/postgresql.sql
INSERT INTO flash_items (id, name, price, original_price, total_stock, stock, start_time, end_time, create_time, update_time, deleted) VALUES
(1, '旗舰手机 Pro', 699900, 899900, 50, 50,
 CURRENT_TIMESTAMP - INTERVAL '10' MINUTE, CURRENT_TIMESTAMP + INTERVAL '1' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '无线降噪耳机', 79900, 129900, 5, 5,
 CURRENT_TIMESTAMP - INTERVAL '10' MINUTE, CURRENT_TIMESTAMP + INTERVAL '1' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, '智能手表限量款', 149900, 199900, 20, 20,
 CURRENT_TIMESTAMP + INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '2' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, '游戏主机套装', 299900, 399900, 10, 10,
 CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
```

### backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/FlashItem.java（新增）

```java
package com.fullstack.demo.domain.flash;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fullstack.common.mybatisplus.entity.BaseIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体（场景 03：秒杀抢购）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flash_items")
public class FlashItem extends BaseIdEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品名
     */
    private String name;

    /**
     * 秒杀价（单位：分）
     */
    private Long price;

    /**
     * 原价（单位：分）
     */
    private Long originalPrice;

    /**
     * 初始库存（对账基准：成功订单数 + stock 应恒等于 total_stock）
     */
    private Integer totalStock;

    /**
     * 剩余库存
     */
    private Integer stock;

    /**
     * 开抢时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
```

### backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/FlashOrder.java（新增）

```java
package com.fullstack.demo.domain.flash;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fullstack.common.mybatisplus.entity.BaseIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 秒杀订单实体（场景 03：秒杀抢购）。
 * <p>
 * (item_id, user_id) 唯一索引：每人限购一件的物理保证；
 * item_name / price 为下单时快照，商品后续改名改价不影响已下订单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flash_orders")
public class FlashOrder extends BaseIdEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 秒杀商品 ID
     */
    private Long itemId;

    /**
     * 下单用户 ID
     */
    private Long userId;

    /**
     * 商品名快照
     */
    private String itemName;

    /**
     * 成交价快照（单位：分）
     */
    private Long price;

    /**
     * 订单状态：0=已抢购（预留 1=已取消/回补库存）
     */
    private Integer status;
}
```

### backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/gateway/FlashItemGateway.java（新增）

```java
package com.fullstack.demo.domain.flash.gateway;

import com.fullstack.demo.domain.flash.FlashItem;

import java.util.List;

/**
 * 秒杀商品领域网关（仓储接口）。
 * <p>
 * 接口位于 domain，实现位于 demo-infrastructure（FlashItemGatewayImpl）。
 */
public interface FlashItemGateway {

    /**
     * 全量商品（按 id 升序）。
     */
    List<FlashItem> findAll();

    /**
     * 按主键查询。
     */
    FlashItem findById(Long id);

    /**
     * 原子扣减一件库存（防超卖第二道防线）。
     * <p>
     * 单语句完成「判断 + 扣减」：UPDATE ... SET stock = stock - 1
     * WHERE id = ? AND stock > 0。返回影响行数：1=成功；0=此刻库存为 0（或商品不存在）。
     * 调用方只认影响行数，不认先前查到的库存值。
     */
    int deductStock(Long itemId);
}
```

### backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/flash/gateway/FlashOrderGateway.java（新增）

```java
package com.fullstack.demo.domain.flash.gateway;

import com.fullstack.demo.domain.flash.FlashOrder;

import java.util.List;

/**
 * 秒杀订单领域网关（仓储接口）。
 */
public interface FlashOrderGateway {

    /**
     * 新增订单（回填雪花 ID；撞 (item_id, user_id) 唯一索引抛 DuplicateKeyException）。
     */
    void insert(FlashOrder order);

    /**
     * 查询用户在某商品上的订单（限购查重用，仅优化非兜底）。
     */
    FlashOrder findByItemAndUser(Long itemId, Long userId);

    /**
     * 用户的全部订单（创建时间倒序）。
     */
    List<FlashOrder> findByUserIdOrderByCreateTimeDesc(Long userId);
}
```

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/FlashItemMapper.java（新增）

```java
package com.fullstack.demo.infrastructure.mapper;

import com.fullstack.common.mybatisplus.mapper.BaseMapperPlus;
import com.fullstack.demo.domain.flash.FlashItem;

/**
 * 秒杀商品 Mapper
 * <p>
 * SQL 位于同包名 XML：resources/mapper/FlashItemMapper.xml。
 */
public interface FlashItemMapper extends BaseMapperPlus<FlashItem> {

    /** 防超卖核心：判断与扣减在 XML SQL 中同一条语句内原子完成。 */
    int deductStock(Long itemId);

    /** 演示专用：库存回满到初始值。 */
    int resetStock(Long itemId);
}
```

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/FlashOrderMapper.java（新增）

```java
package com.fullstack.demo.infrastructure.mapper;

import com.fullstack.common.mybatisplus.mapper.BaseMapperPlus;
import com.fullstack.demo.domain.flash.FlashOrder;

/**
 * 秒杀订单 Mapper
 */
public interface FlashOrderMapper extends BaseMapperPlus<FlashOrder> {
}
```

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/FlashItemGatewayImpl.java（新增）

```java
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
}
```

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/gateway/FlashOrderGatewayImpl.java（新增）

```java
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
}
```

### scenarios/03-flash-sale/sql/postgresql.sql（新增）

```sql
-- 场景 03：秒杀抢购 PostgreSQL 手工初始化脚本
-- 在场景 01 users 表基础上执行（见 scenarios/01-auth/sql/postgresql.sql）；金额单位为分
CREATE TABLE IF NOT EXISTS flash_items (
    id             BIGINT       NOT NULL,
    name           VARCHAR(100) NOT NULL,
    price          BIGINT       NOT NULL,
    original_price BIGINT       NOT NULL,
    total_stock    INT          NOT NULL,
    stock          INT          NOT NULL,
    start_time     TIMESTAMP    NOT NULL,
    end_time       TIMESTAMP    NOT NULL,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    create_by      BIGINT,
    update_by      BIGINT,
    deleted        INT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_flash_items PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flash_orders (
    id          BIGINT       NOT NULL,
    item_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    item_name   VARCHAR(100) NOT NULL,
    price       BIGINT       NOT NULL,
    status      INT DEFAULT 0 NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     INT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_flash_orders PRIMARY KEY (id),
    CONSTRAINT uk_flash_orders_item_user UNIQUE (item_id, user_id)
);

-- 种子数据：时间相对执行时刻生成（PostgreSQL 方言）
INSERT INTO flash_items (id, name, price, original_price, total_stock, stock, start_time, end_time, create_time, update_time, deleted) VALUES
(1, '旗舰手机 Pro', 699900, 899900, 50, 50,
 now() - interval '10 minutes', now() + interval '1 day', now(), now(), 0),
(2, '无线降噪耳机', 79900, 129900, 5, 5,
 now() - interval '10 minutes', now() + interval '1 day', now(), now(), 0),
(3, '智能手表限量款', 149900, 199900, 20, 20,
 now() + interval '1 day', now() + interval '2 days', now(), now(), 0),
(4, '游戏主机套装', 299900, 399900, 10, 10,
 now() - interval '2 days', now() - interval '1 day', now(), now(), 0);
```

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 全部通过（既有用例不受影响，DDL/种子为追加） |
| manual | 启动后 `curl http://localhost:8080/api/health` 正常，日志无 SQL init 报错 | 建表与种子执行成功 |

## 风险与阻塞

- 风险：data.sql 的 H2 `INTERVAL` 语法若版本差异不识别，改为 `DATEADD`；种子固定 id 1-4 与雪花 ID 无碰撞风险。
- 阻塞：无。
- 执行记录：2026-09-07 全部落盘；`mvn test` 通过（20/20，含场景 03 新用例）。修订：1 - postgresql.sql 按 01-auth 脚本惯例补执行方式/前置/回滚注释。
