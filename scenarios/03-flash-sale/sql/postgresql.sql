-- 场景 03：秒杀抢购 PostgreSQL 手工初始化脚本
-- 执行方式：psql -d full_stack_demo -f scenarios/03-flash-sale/sql/postgresql.sql
-- 前置：先执行场景 01 脚本（users 表）；回滚：DROP TABLE flash_orders, flash_items;
-- 金额单位为分（BIGINT）
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

-- 种子数据：四档状态各一件（进行中×2 / 未开始 / 已结束），时间相对执行时刻生成
INSERT INTO flash_items (id, name, price, original_price, total_stock, stock, start_time, end_time, create_time, update_time, deleted) VALUES
(1, '旗舰手机 Pro', 699900, 899900, 50, 50,
 now() - interval '10 minutes', now() + interval '1 day', now(), now(), 0),
(2, '无线降噪耳机', 79900, 129900, 5, 5,
 now() - interval '10 minutes', now() + interval '1 day', now(), now(), 0),
(3, '智能手表限量款', 149900, 199900, 20, 20,
 now() + interval '1 day', now() + interval '2 days', now(), now(), 0),
(4, '游戏主机套装', 299900, 399900, 10, 10,
 now() - interval '2 days', now() - interval '1 day', now(), now(), 0);
