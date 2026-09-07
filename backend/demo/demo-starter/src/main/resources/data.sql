-- 场景 01 种子数据：演示账号 demo / demo123456
-- password 为 BCryptPasswordEncoder 哈希（强度 10）
INSERT INTO users (id, username, password, nickname, create_time, update_time, deleted)
VALUES (1, 'demo', '$2a$10$jxs6NNQ2r2uCMyxyPbhJe.Z8qxDSAWOP9EQrhzdy/wOOeULkaX5.m', '演示账号', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP, 0);

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
