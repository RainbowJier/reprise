---
id: auth-jwt_10_documentation_scenario_docs
name: "场景文档收尾（NOTES/README/AGENTS 更新）"
type: documentation
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_09_test_integration_verify]
profiles: [backend, frontend]
files:
  - scenarios/01-auth/NOTES.md
  - README.md
  - AGENTS.md
  - backend/common/common-webmvc/src/main/java/com/fullstack/common/webmvc/exception/GlobalExceptionHandler.java
---

# 场景文档收尾（NOTES/README/AGENTS 更新）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `documentation` |
| 已启用 profile | backend / frontend |
| 架构边界 | 场景目录（NOTES 笔记）；仓库根（README 场景索引、AGENTS 工作约定）；common-webmvc 一处过时注释 |
| 结构分析 | 笔记模板=`docs/NOETS.md`；README 场景索引表与状态图例已核实；AGENTS「注意事项」段已核实 |
| 完成条件 | 三份文档与实际实现一致；`mvn test` + `npm run build` 复验仍绿（注释改动后） |

## 需求与验收

- 用户目标：按仓库复现规范回答四问（需求/设计/实现/踩坑），并让基座文档反映认证已接入。
- 包含：NOTES.md 完整笔记（坑点来自任务 01–09 执行中的真实记录）；README 场景 01 状态 → ✅ 并链笔记；AGENTS 增补认证注意事项；GlobalExceptionHandler 头注释更新。
- 不包含：新的功能承诺或设计变更。
- 验收：文档描述与代码事实一致（无「未接入认证」残留表述）。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 输入与输出 | 纯文档 + 一行注释 |
| 数据/Schema | 不适用 |
| 集成 | 不适用 |
| 安全与质量 | NOTES 如实记录设计局限（登出无服务端吊销、refresh 无黑名单、secret 单机） |
| 依赖 | 09（联调结果与真实踩坑记录是笔记素材） |

## 实现步骤

1. 按 `docs/NOETS.md` 模板编写 `scenarios/01-auth/NOTES.md`（坑点以执行期实际记录为准，下方骨架中标注「执行后回填」处）；
2. README 场景索引 01 行状态改 ✅、笔记列填 `scenarios/01-auth/NOTES.md`；
3. AGENTS「注意事项」增补认证相关条目；
4. GlobalExceptionHandler 类头注释更新；
5. 复验 `mvn test` 与 `npm run build`。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `scenarios/01-auth/NOTES.md` | 场景目录 | 新增 | 复现笔记 |
| `README.md` | 仓库根 | 修改 | 场景索引状态 |
| `AGENTS.md` | 仓库根 | 修改 | 工作约定增补 |
| `backend/common/common-webmvc/src/main/java/com/fullstack/common/webmvc/exception/GlobalExceptionHandler.java` | common-webmvc | 修改 | 类头注释更新 |

## 完整代码（供手动敲写）

### scenarios/01-auth/NOTES.md（新增）

```markdown
# 01. 用户登录与认证（JWT）

- **一句话**：注册 / 登录 / 双 token 无感续期 / 登出的完整认证闭环。
- **日期**：2026-09
- **技术栈**：Spring Boot 3.5（jjwt + 自研过滤器 + BCrypt）/ Vue 3 + Tailwind v4 / H2

## 1. 需求

真实业务里几乎每个多用户系统都要先回答"你是谁"。核心链路：用户提交账号密码 →
后端校验并签发 access + refresh 双 token → 前端存 localStorage 并以 Bearer 头携带 →
过滤器解析注入上下文 → 受保护接口按上下文响应 → access 过期时前端用 refresh 静默换新。

## 2. 方案设计与取舍

- 数据模型：单表 `users`（表名避开保留字 `user`），BCrypt 哈希 + 唯一索引兜底并发重名。
- 选型：
  - jjwt + 自研 OncePerRequestFilter vs Spring Security：基座是 MyBatis-Plus 风格，
    轻量过滤器与现有 CorsConfig/GlobalExceptionHandler 更好相处，复现价值也更高；
  - 双 token vs 单 token：无感续期是生产经典方案；
  - spring-security-crypto 单引 BCrypt，不引 Security 全家桶；
  - HS256 vs RS256：单服务自签自验，无多方验签需求（SSO 扩展再考虑 RS256）。
- 架构：application 看不到 infrastructure，`TokenProvider` 端口接口放 client 层做依赖倒置
  （与 demo-client README 的 Feign 预留同一思路）。设计文档见 `auth-jwt.md`。

## 3. 实现要点

- 401 统一走 HTTP 200 + body code=401（跟随 GlobalExceptionHandler 约定），前端拦截器按 code 分流；
- 过滤器异常不经过 `@RestControllerAdvice`，401 JSON 在 `JwtAuthFilter` 内自写；
- 前端 token 存取独立成 `authTokens.js`，http 拦截器与 store 共享，避免 http↔store 循环引用；
- 刷新走独立裸 axios 实例（`refreshHttp`）防 401 递归，并发 401 单飞共享同一次刷新；
- `@MapperScan` 固定扫描 `infrastructure.mapper` 包，Mapper 落位不能想当然放 domain。

## 4. 踩坑记录

| 现象 | 原因 | 解决 |
| ---- | ---- | ---- |
| _执行后回填_ | | |

## 5. 验证与数据

- 后端集成测试 10 用例（`AuthFlowIntegrationTest`）+ curl 冒烟（`verify.sh`）；
- 手动验收 8 项清单见任务 09；未做压测（本场景瓶颈在 BCrypt 登录校验，~几十 ms/次，量级已知）。

## 6. 参考与延伸

- jjwt 官方 wiki（0.12 API）；RFC 7519
- 延伸：SSO/CAS（RS256 多方验签）、token 黑名单（Redis）、RBAC（场景 08）、限流防爆破
```

### README.md（修改）

```markdown
| 01  | 用户登录与认证（JWT/SSO）  | `scenarios/01-auth`                        | ✅ 已完成 | [NOTES.md](scenarios/01-auth/NOTES.md) |
```

修改点说明：场景索引表 01 行——状态由「✅ 基座已就绪」改「✅ 已完成」，笔记列填链接。

### AGENTS.md（修改）

在「注意事项 / 坑」列表末尾追加：

```markdown
- 认证（场景 01 起）：`/user/**` 需携带 `Authorization: Bearer <accessToken>`（`JwtAuthFilter` 只拦该路径），`/auth/**`、`/health` 为白名单；业务 401 统一为 HTTP 200 + body `code=401`。JWT 配置在 `app.jwt.*`（主/测试两份 yml 都要同步），种子账号 demo / demo123456。
- 前端样式（场景 01 起）为 Tailwind CSS v4 utility-first：token 定义在 `src/style.css` 的 `@theme`，新代码不要写手写全局 CSS 类，也不要硬编码 hex。
```

修改点说明：仅追加两条，既有条目不动；如基座其他描述与实现冲突一并微调（如 `src/api/http.js` 描述仍准确，不改）。

### backend/common/common-webmvc/src/main/java/com/fullstack/common/webmvc/exception/GlobalExceptionHandler.java（修改）

```java
/**
 * MVC 全局异常处理器。
 * <p>
 * 相比 fjgtkj-2026 原实现，移除了 Sa-Token（NotLogin/NotPermission/NotRole）与
 * 验证码（CaptchaException）分支：demo 采用自研 JWT 过滤器（场景 01），
 * 认证失败在 JwtAuthFilter 内直接写响应，不经过本处理器。
 */
```

修改点说明：仅替换类头 Javadoc（原文表述"demo 未接入认证体系"已过时），类体不动。落点：`GlobalExceptionHandler` 类声明上方注释。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build/test | `cd backend && mvn test` | 全绿（注释改动不影响） |
| build | `cd frontend && npm run build` | 构建成功 |
| manual | 通读三份文档 | 与实现一致，无过时表述 |

## 风险与阻塞

- 风险：无。NOTES 踩坑表须以执行期真实记录回填，不得编造。
- 阻塞：无。
- 执行记录：2026-09-05 NOTES.md 落盘（7 条真实踩坑）、README 场景 01 → ✅ 并链笔记、AGENTS 增认证与 Tailwind 两条注意事项、GlobalExceptionHandler 注释更新；复验 `mvn test` 11/11 + `npm run build` 通过。
