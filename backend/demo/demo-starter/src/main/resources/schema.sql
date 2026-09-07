-- 场景 01：用户登录与认证（JWT）users 表
-- H2 内嵌库启动时自动执行（spring.sql.init 对 EMBEDDED 默认开启，含测试 classpath）；
-- PostgreSQL 不自动执行，手工脚本见 scenarios/01-auth/sql/postgresql.sql
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
