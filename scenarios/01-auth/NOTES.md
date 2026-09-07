# 01. 用户登录与认证（JWT）

- **一句话**：注册 / 登录 / 双 token 无感续期 / 登出的完整认证闭环。
- **日期**：2026-09
- **技术栈**：Spring Boot 3.5（jjwt 0.13 + 自研过滤器 + BCrypt）/ Vue 3 + Tailwind v4 / H2

## 1. 需求

真实业务里几乎每个多用户系统都要先回答"你是谁"。核心链路：用户提交账号密码 →
后端校验并签发 access + refresh 双 token → 前端存 localStorage 并以 Bearer 头携带 →
过滤器解析注入上下文 → 受保护接口按上下文响应 → access 过期时前端用 refresh 静默换新。

## 2. 方案设计与取舍

- 数据模型：单表 `users`（表名避开保留字 `user`），BCrypt 哈希 + 唯一索引兜底并发重名。
- 选型：
  - jjwt + 自研 OncePerRequestFilter vs Spring Security：基座是 MyBatis-Plus 风格，
    轻量过滤器与现有 CorsConfig/GlobalExceptionHandler 更好相处，复现价值也更高；
  - 双 token vs 单 token：无感续期是生产经典方案；刷新时旋转双发（新对签发、旧对自然过期）；
  - spring-security-crypto 单引 BCrypt，不引 Security 全家桶（无过滤链自动配置干扰）；
  - HS256 vs RS256：单服务自签自验，无多方验签需求（SSO 扩展再考虑 RS256）。
- 架构：application 层看不见 infrastructure，两处端口反转——
  `TokenProvider` 端口在 client（jjwt 实现在 infrastructure）、
  `UserGateway` 端口在 domain（`UserGatewayImpl` 委托 `UserMapper`）。
  设计文档与配图见 `auth-jwt.md`、`auth-jwt/`。

## 3. 实现要点

- 401 统一走 HTTP 200 + body `code=401`（跟随 GlobalExceptionHandler 约定），前端拦截器按 code 分流；
- 过滤器异常不经过 `@RestControllerAdvice`，401 JSON 在 `JwtAuthFilter` 内自写（注入 ObjectMapper 保持序列化一致）；
- 过滤器不用 `@Component`，由 `FilterRegistrationBean` 限定只拦 `/user/*`，`/auth/**`、`/health` 天然白名单；
- 前端 token 存取独立成 `authTokens.js` 纯模块，http 拦截器与 store 共享，避免 http↔store 循环引用；
- 刷新走独立裸 axios 实例（`refreshHttp`）防 401 递归，并发 401 单飞共享同一次刷新，`__retried` 标记防重放死循环。

## 4. 踩坑记录

| 现象 | 原因 | 解决 |
| ---- | ---- | ---- |
| application 注入 UserMapper 编译报"程序包不存在" | AGENTS 依赖方向 application 不可见 infrastructure，而 `@MapperScan` 只扫 `infrastructure.mapper` | domain 定义 `UserGateway` 端口、infrastructure 用 `UserGatewayImpl` 实现，application 只依赖接口 |
| jjwt 依赖解析失败：连 192.168.x.x 超时 | 用户级与安装级两份 settings.xml 都配了同一个内网镜像（当前不可达），`-s` 只覆盖用户级 | 临时 settings 同时 `-s`/`-gs` 从 Central 下载；注意 `_remote.repositories` 记录的仓库 ID 与镜像配置不匹配时本地已有构件也会重新解析 |
| 集成测试 404"No static resource" | TestRestTemplate 自动携带 context-path，测试路径再写 `/api` 变成双重前缀 | 测试路径只写 servlet 内路径（`/auth/login`），不带 `/api` |
| 同一秒内登录两次，两次 access token 完全相同 | jjwt 的 iat/exp 只有秒级精度，claims 全等 → HMAC 签名全等，JWT 是确定性输出 | 签发时加 `jti`（UUID）claim 保证字节级唯一（也是未来黑名单吊销的挂载点） |
| 种子账号昵称"演示账号"前端显示乱码 | `data.sql` 是 UTF-8，Windows 下 `spring.sql.init` 默认按平台编码（GBK）读脚本 | `spring.sql.init.encoding: UTF-8`（主/测试 yml 都要加） |
| curl 冒烟脚本注册返回 417"请求体格式错误" | `-d` 里的中文载荷在 Windows 控制台编码下被损坏成非法 JSON | 脚本载荷一律 ASCII；顺带修了 `set -e` 下 `test A && test B` 非末位失败不退出的陷阱（改显式 `if` 守卫） |
| 用健康检查按钮验证"无感续期"永远成功 | `/health` 在白名单，不携带 token 也不经过过滤器，根本走不到 401 分支 | 验证载体换成受保护接口：页面刷新触发 `fetchUser()`（/user/me）才真正走 401→refresh→重放链路 |
| 路由切换加 `<Transition mode="out-in">` 后页面偶发空白/卡旧页 | 过渡状态机与懒加载视图组件组合不可靠：被打断时离场不完成（残留旧页），补 `:duration` 后又出现 enter 不触发（main 只剩注释占位），且无任何控制台报错 | 放弃组件级 Transition：视图根元素自带 `animate-fade-up` CSS 入场动画（挂载即播，无可卡状态），90ms 中途连击实测稳定 |

## 5. 验证与数据

- 后端集成测试 11/11（`AuthFlowIntegrationTest` 10 用例 + contextLoads）；
- curl 冒烟 `verify.sh` 7/7（注册/me/无 token 401/伪造 401/旋转刷新/demo 登录/文案统一）；
- 浏览器手动验收 8/8（守卫重定向、注册即登录、F5 保持登录、登出、demo 登录、密码错误提示、无感续期、全过期跳登录——后两项以临时 TTL 30s/90s 实测，验后已恢复）；
- 未做压测：本场景瓶颈在登录 BCrypt 校验（~几十 ms/次，量级已知），过滤器验签为微秒级。

## 6. 参考与延伸

- jjwt 官方 wiki（0.12+ API）；RFC 7519
- 延伸：SSO/CAS（RS256 多方验签）、token 黑名单（Redis，按 jti）、RBAC（场景 08）、登录限流防爆破
