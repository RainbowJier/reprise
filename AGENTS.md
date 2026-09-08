# AGENTS.md

reprise：复现经典前后端场景的实践仓库。所有场景共用一套基座（`backend/` + `frontend/`），场景代码直接在基座上迭代；`scenarios/`（按需创建）只存放各场景的说明与笔记。当前仓库仅含初始化基座，尚无业务功能。

## 常用命令

后端（Java 17 / Spring Boot 3.5 / Maven 多模块，工作目录 `backend/`）：

```bash
cd backend
mvn test                                        # 全量测试（随机 H2 内存库，无需外部数据库）
mvn clean package                               # 仅 demo-starter 生成可运行 jar（父 POM 默认 skip repackage）
java -jar demo/demo-starter/target/demo-starter-0.0.1-SNAPSHOT.jar   # 端口 8080，context-path /api
curl http://localhost:8080/api/health           # 健康检查验证；IDE 可直接运行 com.fullstack.demo.starter.DemoApplication
```

前端（Vue 3 + Vite 8 + JavaScript ESM，工作目录 `frontend/`）：

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173，/api 代理到 8080（无 rewrite）
npm run build    # 生产构建，也是目前唯一的前端校验手段（无 lint / 测试配置）
```

数据库：默认 H2 内存库，零配置；PostgreSQL 需 `--spring.profiles.active=postgresql`（见 `application-postgresql.yml`，库名 `full_stack_demo`，表结构由各场景手工初始化）。

## 架构与分层规则

后端 = `common/` 组件工程 + `demo/` DDD 七层服务工程（参考 fjgtkj-2026 架构）。模块依赖方向不得反向：

- `demo-shared` ← `demo-domain` ← `demo-infrastructure`
- `demo-domain` + `demo-client` ← `demo-application`
- `demo-application` + `demo-client` ← `demo-adapter`（Controller 都在 adapter 层）
- `demo-adapter` + `demo-infrastructure` ← `demo-starter`（唯一启动入口与可执行 jar）

分层落位：`demo-client` 的 `client/service/` 定义服务接口，实现放在 `demo-application`；application 层禁止直接注入他服务的 client（Feign 契约预留于 `client/feign/`，接入微服务时由 infrastructure 的 Gateway 实现调用，详见 `backend/demo/demo-client/README.md`）。

## 编码约定

后端：

- Controller 返回统一响应 `AjaxResult<T>`（`{code, msg, data}`，成功码 200）；业务错误抛 `BusinessException`，由 common-webmvc 的 `GlobalExceptionHandler` 统一处理，不要在 Controller 里自行 try/catch 拼错误响应。
- 实体继承 `BaseEntity`（createTime/updateTime/createBy/updateBy 自动填充，`deleted` 逻辑删除 0/1）；Mapper 继承 `BaseMapperPlus`；分页用 `PageQuery` / `PageQueryResp`。
- 三方依赖版本统一在根 `backend/pom.xml` 的 dependencyManagement 管理（MyBatis-Plus、hutool、mapstruct 等）。
- `demo-starter/src/test/resources/application.yml` 会完整遮蔽主配置（同名文件覆盖），修改主配置时需同步检查测试配置。

前端：

- `src/api/http.js` 是统一 axios 封装（自动解包 `data.data`，`code !== 200` 时 reject）；新接口按模块放在 `src/api/` 下。
- 路径别名 `@` → `src/`。
- 管理端布局（RuoYi 风格）：`src/layouts/AdminLayout.vue` 左侧场景菜单 + 右侧详情页，挂在 `/` 布局父路由（requiresAuth）；每个场景父菜单下两个子菜单——「详情页效果展示」（`/scenario/NN-xxx`）与「技术说明文档」（`/scenario/NN-xxx/doc`，通用 `DocView` 解析 md 渲染，展示场景设计文档 `scenarios/NN-xxx/design.md`）。新场景接入三步：`src/config/scenarios.js` 置 `enabled: true`、router 布局 children 成对追加两条路由（文档路由传 `props: { scenarioId }`）、`src/config/scenarioDocs.js` 登记 design.md `?raw` 与 SVG `?url` 资源映射；登录/注册为独立全屏页。

通用：注释、文档、commit message 均使用中文；commit 风格为 `feat: xxx`。

## 注意事项 / 坑

- 后端 context-path 是 `/api`，前端 Vite 代理不做 rewrite——接口路径一律从 `/api` 起头。
- CORS 白名单在 `app.cors.allowed-origins`（application.yml）：Vite 在 5173 被占用时会自动顺延到 5174，白名单已含 5173/5174/3000；新增前端端口需同步加白，否则代理 POST 会被 403 拒绝。
- `scenarios/` 只放场景资产（设计文档 `{slug}.md`、技术讲义 `design.md`、`tasks/`、`sql/`、`verify.sh`），不要在其中创建前后端工程；场景状态变化时同步更新 README 的场景索引表。双文档分工（fullstack-feature 阶一产物与前端展示分离）：`{slug}.md` 是设计文档——面向开发，含需求/取舍/数据模型/契约/影响范围与「踩坑与实测」节（见 `01-auth/auth-jwt.md`、`03-flash-sale/flash-sale.md`）；`design.md` 是纯场景讲义——前端「技术说明文档」页（DocView）渲染用，**不含项目相关内容**（仓库、模块、类名、实测数据一律不出现），`scenarioDocs.js` 只登记 design.md。
- 认证（场景 01 起）：`/user/**`、`/flash/**`（场景 03 起）需携带 `Authorization: Bearer <accessToken>`（`JwtAuthFilter` 只拦这两类路径，注册处在 `AuthFilterConfig`），`/auth/**`、`/health` 为白名单；业务 401 统一为 HTTP 200 + body `code=401`。JWT 配置在 `app.jwt.*`（主/测试两份 yml 都要同步），种子账号 demo / demo123456；冒烟脚本 `bash scenarios/01-auth/verify.sh`、`bash scenarios/03-flash-sale/verify.sh`（秒杀冒烟会耗尽种子库存，重复运行需重启后端；可用 `FLASH_BASE` 换端口）。
- 秒杀（场景 03）：防超卖依赖 `flash_orders` 的 `uk(item_id, user_id)` 唯一索引与 `FlashItemMapper.deductStock` 原子条件更新，改动表结构或扣减语句前先看 `scenarios/03-flash-sale/design.md` 第三节；秒杀业务错误码为 61xx 分段（6101 未开始/6102 已结束/6103 已售罄/6104 重复抢购）；金额字段一律 BIGINT 存分。`POST /flash/demo/reset/{itemId}` 为演示专用重置接口（库存回满 + 物理清订单 + 清售罄标记，仅进行中商品），供前端并发演示控制台循环使用，生产环境不可保留。
- 前端样式（场景 01 起）为 Tailwind CSS v4 utility-first：token 定义在 `src/style.css` 的 `@theme`，新代码不要写手写全局 CSS 类，也不要硬编码 hex。

## 相关文档 / 工具

- `README.md`：场景索引与本地运行说明。
- `backend/demo/demo-client/README.md`：client 层契约与 Feign 预留规则。
- 工作区 skill：`fullstack-feature`（Vue + Spring Boot 全栈新功能工作流，做新功能时优先触发）。
