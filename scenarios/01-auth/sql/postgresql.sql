-- 场景 01：用户登录与认证（JWT）PostgreSQL 手工初始化脚本
-- 执行方式：psql -d full_stack_demo -f scenarios/01-auth/sql/postgresql.sql
-- 回滚：DROP TABLE users;
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     INT          DEFAULT 0 NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- 演示账号 demo / demo123456（哈希值与 data.sql 保持一致）
INSERT INTO users (id, username, password, nickname, create_time, update_time, deleted)
VALUES (1, 'demo', '$2a$10$jxs6NNQ2r2uCMyxyPbhJe.Z8qxDSAWOP9EQrhzdy/wOOeULkaX5.m', '演示账号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
