-- 场景 01 种子数据：演示账号 demo / demo123456
-- password 为 BCryptPasswordEncoder 哈希（强度 10）
INSERT INTO users (id, username, password, nickname, create_time, update_time, deleted)
VALUES (1, 'demo', '$2a$10$jxs6NNQ2r2uCMyxyPbhJe.Z8qxDSAWOP9EQrhzdy/wOOeULkaX5.m', '演示账号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
