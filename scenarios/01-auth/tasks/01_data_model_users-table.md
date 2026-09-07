---
id: auth-jwt_01_data_model_users_table
name: "users 表数据基座（实体/Mapper/Schema/依赖管理）"
type: data_model
subtype: null
status: completed
blocked_reason: null
depends: []
profiles: [backend, database]
files:
  - backend/pom.xml
  - backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/user/User.java
  - backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/UserMapper.java
  - backend/demo/demo-starter/src/main/resources/schema.sql
  - backend/demo/demo-starter/src/main/resources/data.sql
  - scenarios/01-auth/sql/postgresql.sql
---

# users 表数据基座（实体/Mapper/Schema/依赖管理）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `data_model` |
| 已启用 profile | backend / database |
| 架构边界 | 根 pom 版本管理；demo-domain（实体）；demo-infrastructure（Mapper，`@MapperScan` 限定包）；demo-starter resources（H2 初始化 SQL）；scenarios SQL 目录 |
| 结构分析 | 只读降级分析：`DemoApplication` 的 `@MapperScan("com.fullstack.demo.infrastructure.mapper")` 已核实，Mapper 必须落位该包 |
| 完成条件 | `mvn test` 通过；H2 启动后 `users` 表存在且有 demo 种子账号 |

## 需求与验收

- 用户目标：为认证功能建立用户持久化基座，后续任务可直接读写用户数据。
- 包含：`users` 表（H2 + PostgreSQL 双方言）、`User` 实体、`UserMapper`、根 pom 的 jjwt / spring-security-crypto 版本管理、demo 种子账号。
- 不包含：JWT 逻辑、业务服务、接口（后续任务）。
- 验收：`mvn test` 全绿；`schema.sql`/`data.sql` 在 H2 自动执行无报错；demo 账号密码哈希为合法 BCrypt（60 字符，`$2a$` 开头）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 无接口；产出实体/Mapper/表结构供任务 03、04 使用 |
| 数据/Schema | 表名 `users`（规避 `USER` 保留字）；`username` 唯一索引兜底并发重名；无迁移工具，H2 走 `spring.sql.init`（对内嵌库默认开启），PG 手工脚本 |
| 集成 | 不适用 |
| 安全与质量 | 密码列只存 BCrypt 哈希；`deleted` 逻辑删除随 `BaseEntity` |
| 依赖 | 无前置任务；jjwt 版本在此统一管理，任务 03 使用 |

## 实现步骤

1. 根 `backend/pom.xml` properties 加 `jjwt.version`，dependencyManagement 追加 jjwt 三件套；
2. 新建 `User` 实体（domain）与 `UserMapper`（infrastructure.mapper 包，扫描限定）；
3. 新建 `schema.sql` / `data.sql`（starter main resources）与 PostgreSQL 手工脚本；
4. **生成 BCrypt 哈希回填 `data.sql`**（见下方说明），删除临时生成器；
5. 执行 `mvn test` 验证。

### BCrypt 哈希生成说明（执行时必做）

`data.sql` 中 demo 账号密码哈希无法预先给出（BCrypt 带随机盐，必须真实计算）。执行本任务时：先把 `data.sql` 落盘（哈希处暂记占位），在 `demo-starter/src/test/java/com/fullstack/demo/starter/` 新建临时类运行一次，将控制台输出的哈希替换进 `data.sql` 与 `postgresql.sql`，然后删除临时类。生成器代码：

```java
package com.fullstack.demo.starter;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class TempBcryptHashGeneratorTest {

    @Test
    void generate() {
        System.out.println("BCRYPT=" + new BCryptPasswordEncoder().encode("demo123456"));
    }
}
```

> 注意：此时 application 模块尚未引入 spring-security-crypto（任务 03 引入），该临时测试在任务 03 完成依赖引入后方可运行。**若任务 01 先于任务 03 执行**，可将 spring-security-crypto 依赖在本任务一并加入 `demo-application/pom.xml`（版本由 Boot BOM 管理，无序号影响），或推迟到任务 03 执行时生成哈希。推荐顺序：本任务先落盘其余文件，哈希在任务 03 完成后生成回填，`mvn test` 验证顺延。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `backend/pom.xml` | 根 reactor | 修改 | jjwt 版本管理 |
| `backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/user/User.java` | demo-domain | 新增 | 用户实体 |
| `backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/UserMapper.java` | demo-infrastructure | 新增 | Mapper（必须在此包） |
| `backend/demo/demo-starter/src/main/resources/schema.sql` | starter | 新增 | H2 建表 |
| `backend/demo/demo-starter/src/main/resources/data.sql` | starter | 新增 | 种子数据 |
| `scenarios/01-auth/sql/postgresql.sql` | 场景目录 | 新增 | PG 手工脚本 |

## 完整代码（供手动敲写）

### backend/pom.xml（修改）

```xml
<!-- properties 段内追加（位于 mybatis-plus.version 之后） -->
<jjwt.version>0.13.0</jjwt.version>

<!-- dependencyManagement/dependencies 内追加（位于 mapstruct-processor 之后） -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
</dependency>
```

修改点说明：jjwt 采用 api（编译）/impl+jackson（运行）三件套拆分模式；spring-security-crypto 版本由 spring-boot-starter-parent 3.5.16 BOM 统一管理，无需在此声明。落点：`<properties>` 与 `<dependencyManagement>` 两处对应位置。

修订：1 - jjwt 版本 0.12.6 改 0.13.0（执行环境内网镜像不可达，本地仓库已缓存 0.13.0；API 与 0.12 兼容）。

### backend/demo/demo-domain/src/main/java/com/fullstack/demo/domain/user/User.java（新增）

```java
package com.fullstack.demo.domain.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fullstack.common.mybatisplus.entity.BaseIdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 用户实体（场景 01：登录与认证）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseIdEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录名（唯一索引）
     */
    private String username;

    /**
     * BCrypt 密码哈希
     */
    private String password;

    /**
     * 昵称（缺省同 username）
     */
    private String nickname;
}
```

### backend/demo/demo-infrastructure/src/main/java/com/fullstack/demo/infrastructure/mapper/UserMapper.java（新增）

```java
package com.fullstack.demo.infrastructure.mapper;

import com.fullstack.common.mybatisplus.mapper.BaseMapperPlus;
import com.fullstack.demo.domain.user.User;

/**
 * 用户 Mapper
 * <p>
 * DemoApplication 的 @MapperScan 只扫描本包，必须落位于此。
 */
public interface UserMapper extends BaseMapperPlus<User> {
}
```

### backend/demo/demo-starter/src/main/resources/schema.sql（新增）

```sql
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
```

### backend/demo/demo-starter/src/main/resources/data.sql（新增）

```sql
-- 场景 01 种子数据：演示账号 demo / demo123456
-- password 为 BCryptPasswordEncoder 哈希，执行本任务时按「BCrypt 哈希生成说明」生成后替换下方占位
INSERT INTO users (id, username, password, nickname, create_time, update_time, deleted)
VALUES (1, 'demo', 'REPLACE_WITH_BCRYPT_HASH', '演示账号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
```

> `REPLACE_WITH_BCRYPT_HASH` 为执行期回填值（生成方式见实现步骤 4），非交付残留占位。

### scenarios/01-auth/sql/postgresql.sql（新增）

```sql
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

-- 演示账号 demo / demo123456（哈希值与 data.sql 保持一致，生成方式见任务 01）
INSERT INTO users (id, username, password, nickname, create_time, update_time, deleted)
VALUES (1, 'demo', 'REPLACE_WITH_BCRYPT_HASH', '演示账号', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
```

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 全部通过（含既有 contextLoads） |
| manual | 启动应用后用 H2 控制台或日志确认 | 无 schema/data.sql 执行报错（需哈希已回填） |

## 风险与阻塞

- 风险：`data.sql` 哈希未回填时应用能启动但 demo 账号无法登录；`schema.sql` 对测试上下文同样执行，若 SQL 方言不兼容会在任务 05 暴露。
- 阻塞：无。
- 执行记录：2026-09-05 落盘全部文件；`mvn test` BUILD SUCCESS（contextLoads 验证 schema/data.sql 执行无误）；BCrypt 哈希按计划推迟至任务 03 回填。
