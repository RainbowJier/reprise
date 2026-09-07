# 场景 01 · 用户登录与认证（JWT）设计文档

> reprise 场景复现系列 · 作者按：本文是场景 01 的成稿设计文档，面向想理解"一个生产级登录认证如何设计"的读者；过程性的任务拆解与执行记录见场景目录其他文件。

## 一、背景与目标

几乎所有多用户系统的第一个问题都是"你是谁"。本场景在基座（Spring Boot 3.5 DDD 七层 + Vue 3）上实现完整的认证闭环：

**注册 → 登录 → 携带 token 访问受保护接口 → access 过期无感续期 → 登出**

核心链路：用户提交账号密码 → 后端校验并签发 access + refresh 双 token → 前端存 localStorage 并以 `Authorization: Bearer` 头携带 → 后端过滤器解析 token 注入用户上下文 → 受保护接口按上下文响应 → access 过期时前端用 refresh 静默换新。

**非目标**（本场景不做，留作延伸）：SSO 单点登录、RBAC 角色权限（场景 08）、验证码与防爆破限流、token 黑名单/服务端吊销、找回密码。

## 二、总体设计

![图2：认证组件在 DDD 分层中的落位与请求链路](auth-jwt/auth-architecture.svg)

### 2.1 分层落位与依赖倒置

后端遵循基座 DDD 七层，其中 `application` 层按依赖规则不可见 `infrastructure`，本场景有两处端口反转：

| 端口接口 | 所在层 | 实现 | 解决的问题 |
| --- | --- | --- | --- |
| `TokenProvider` | demo-client | `JwtTokenProvider`（infrastructure，jjwt） | application 签发/解析 token 不触碰 jjwt 细节 |
| `UserGateway` | demo-domain | `UserGatewayImpl`（infrastructure，委托 `UserMapper`） | application 访问用户数据不依赖 MyBatis |

`UserMapper` 因 `@MapperScan` 固定扫描 `infrastructure.mapper` 包而落位基础设施层——这也是引入 `UserGateway` 端口的直接原因。

### 2.2 请求链路与白名单

- `JwtAuthFilter` 以 `FilterRegistrationBean` 注册，**只拦截 `/user/*`**；`/auth/**`、`/health` 天然不经过过滤器（URL pattern 即白名单，无需维护排除列表）；
- 过滤器不标 `@Component`，避免 Spring 对 Filter 的全路径自动注册造成白名单接口被误拦；
- CORS 预检（OPTIONS）直接放行；过滤器异常不经过 `@RestControllerAdvice`，401 响应在过滤器内自写 JSON，与全局异常处理输出同构（HTTP 200 + body `code=401`）。

## 三、认证流程设计

### 3.1 双 token 生命周期

![图1：双 token 生命周期与 401 无感续期](auth-jwt/dual-token-flow.svg)

| token | 用途 | 有效期 | 存放 |
| --- | --- | --- | --- |
| access | 访问受保护接口 | 30 分钟 | localStorage + Bearer 头 |
| refresh | 过期后换取新 token 对 | 7 天 | localStorage（仅发给 `/auth/refresh`） |

刷新采用**旋转双发**：每次 refresh 同时签发新的 access + refresh，旧对自然过期。无状态 JWT 不查库校验（refresh 场景昵称取不到最新值，是已知取舍）。

### 3.2 前端无感续期

1. 响应拦截器捕获 `code=401` 且本地存在 refreshToken → 挂起原请求，发起**单飞**刷新（并发 401 共享同一次 refresh，防风暴）；
2. 刷新走独立裸 axios 实例，绕开自身拦截器避免 401 递归；
3. 成功 → 更新存储 → 重放原请求（`__retried` 标记防死循环）；失败（refresh 也过期）→ 清空登录态 → 跳 `/login?redirect=原路径`。

### 3.3 token 结构与验签

![图3：JWT 三段结构与 HS256 验签原理](auth-jwt/jwt-structure.svg)

- claims：`sub`=userId、`username`、`type`=access/refresh、`iat`、`exp`、`jti`（UUID）；
- `jti` 保证同一秒内重复签发也字节级不同（jjwt 时间戳只有秒级精度，无 jti 时同秒签发的 token 完全相同），同时是未来黑名单吊销的挂载点；
- `type` claim 防止 access 与 refresh 混用（拿 access 当 refresh 请求 → 401）；
- HS256 对称签名，secret ≥ 32 字节配置于 `app.jwt.*`（弱密钥启动即报错）。

## 四、数据模型

单表 `users`（`USER` 是 H2/PostgreSQL 双方言保留字，直接用必踩坑）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 雪花算法（BaseIdEntity） |
| username | VARCHAR(50) | 唯一索引，4-32 位字母数字下划线 |
| password | VARCHAR(100) | BCrypt 哈希（强度 10，带盐慢哈希） |
| nickname | VARCHAR(50) | 缺省同 username |
| create_time / update_time / deleted | — | BaseEntity 自动填充 + 逻辑删除 |

数据变更遵循基座约定（无迁移工具）：H2 由 `schema.sql`/`data.sql` 启动自动初始化（预置演示账号 demo / demo123456）；PostgreSQL 用 `sql/postgresql.sql` 手工执行。

## 五、接口契约

统一约定：路径含 context-path `/api`；响应包 `AjaxResult{code, msg, data}`；认证失败 HTTP 200 + `code=401`（跟随基座统一响应风格，前端按 code 分流）。

| 接口 | 认证 | 请求 | 成功返回 | 失败 |
| --- | --- | --- | --- | --- |
| POST /auth/register | 白名单 | username / password / nickname? | TokenResp（注册即登录） | 重名 409；参数非法 417 |
| POST /auth/login | 白名单 | username / password | TokenResp | 统一 401「用户名或密码错误」（防用户名枚举） |
| POST /auth/refresh | 白名单 | refreshToken | TokenResp（旋转双发） | 无效/过期/type 不符 401 |
| GET /user/me | Bearer access | — | UserInfoResp | 缺失/过期/伪造 401 |

TokenResp：`accessToken`、`refreshToken`、`tokenType:"Bearer"`、`accessExpiresIn`、`userId`、`username`、`nickname`。

## 六、关键取舍

| 决策点 | 选择 | 理由 |
| --- | --- | --- |
| 认证框架 | jjwt + 自研 OncePerRequestFilter | 贴合 MyBatis-Plus 风格基座；不与现有 CORS/异常处理冲突；复现"手写认证"的学习价值高于"配置框架" |
| 密码哈希 | spring-security-crypto 单引 BCrypt | 独立轻模块（无 Security 过滤链），带盐慢哈希 |
| 签名算法 | HS256 | 单服务自签自验；RS256 留给 SSO 多方验签 |
| 登出语义 | 前端清除存储，无后端接口 | 无状态 JWT 服务端不存会话；黑名单需引入存储，超出场景边界 |
| token 存放 | localStorage + Bearer 头 | 避免 CSRF；XSS 风险依赖前端防护（不加 v-html 渲染不可信内容） |
| 重名并发 | 先查后插 + 唯一索引兜底（DuplicateKeyException → 409） | 竞态窗口由数据库约束封死 |

## 七、安全边界与已知局限

- 登出仅客户端清除，签发过的 token 在过期前仍有效（无黑名单）；
- 旋转前的旧 refresh 在过期前仍可用一次；
- HS256 单机 secret，多实例部署需共享密钥配置；
- 无验证码/限流（错误码 429 已预留）；
- 日志不落密码与 token 原文；登录失败不区分用户名/密码错误。

## 八、验证结果

| 层级 | 方式 | 结果 |
| --- | --- | --- |
| 后端 | `mvn test` 集成测试 10 用例（注册/重名/参数/登录/密码错/无 token/伪造/me/旋转刷新/type 混用） | 11/11 通过 |
| 接口 | curl 冒烟脚本 `verify.sh` 7 步 | 全部通过 |
| 浏览器 | 手动验收 8 项（守卫重定向、注册即登录、刷新保持、登出、demo 登录、错误提示、无感续期、全过期跳转） | 8/8 通过（续期项以临时 TTL 30s/90s 实测） |

## 九、延伸方向

SSO/CAS（RS256 多方验签）· token 黑名单（Redis，按 jti）· RBAC（场景 08）· 登录限流与验证码 · 多端登录互踢。

> 踩坑过程与执行细节见 `NOTES.md`；完整任务拆解见 `tasks/`。
