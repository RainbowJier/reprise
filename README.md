# reprise

> reprise /rəˈpriːz/ · 名词 · 重演，复现

**复现经典前后端场景的实践记录** · Full-stack reproductions of classic web scenarios

每个场景独立成篇：从需求出发，亲手实现一遍后端与前端，记录设计取舍与踩过的坑。

当前仓库已完成前后端基础工程初始化，暂不包含具体业务功能；后续场景在统一基座上按需实现。

## 场景索引

| #   | 场景                       | 目录                                       | 状态  | 笔记 |
| --- | -------------------------- | ------------------------------------------ | ----- | ---- |
| 01  | 用户登录与认证（JWT/SSO）  | `scenarios/01-auth`                        | ✅ 基座已就绪 | —    |
| 02  | 短链接服务                 | `scenarios/02-short-link`                  | ☐ 规划 | —    |
| 03  | 秒杀 / 高并发抢购          | `scenarios/03-flash-sale`                  | ☐ 规划 | —    |
| 04  | Feed 流（推 / 拉 / 推拉结合） | `scenarios/04-feed`                     | ☐ 规划 | —    |
| 05  | 即时通讯（WebSocket）      | `scenarios/05-im`                          | ☐ 规划 | —    |
| 06  | 文件上传（分片 / 秒传）    | `scenarios/06-upload`                      | ☐ 规划 | —    |
| 07  | 支付对接与回调对账         | `scenarios/07-payment`                     | ☐ 规划 | —    |
| 08  | RBAC 权限系统              | `scenarios/08-rbac`                        | ☐ 规划 | —    |
| 09  | 分布式锁与库存扣减         | `scenarios/09-distributed-lock`            | ☐ 规划 | —    |
| 10  | 多级缓存与一致性           | `scenarios/10-caching`                     | ☐ 规划 | —    |

> 状态：☐ 规划 → 🧱 基座已就绪 → 🚧 进行中 → ✅ 已完成。场景代码统一在根目录 `backend/` + `frontend/` 基座上迭代；`scenarios/` 只存放各场景的说明与笔记，按需创建。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.5.16
- Maven 多模块工程
- MyBatis-Plus
- H2（默认开发/测试数据库）与 PostgreSQL 配置
- Jackson、参数校验、统一响应、全局异常处理、CORS
- DDD 分层：`shared`、`domain`、`client`、`infrastructure`、`application`、`adapter`、`starter`

### 前端

- Vue 3
- Vite 8
- JavaScript（ES Modules）
- vue-router
- Pinia
- Axios

## 目录结构

```
reprise/
├── backend/                # Spring Boot 3.5 多模块 Maven 后端基座
│   ├── common/             # 公共组件：统一响应、异常、分页、MyBatis-Plus、Web 配置
│   │   ├── common-base/
│   │   ├── common-mybatis-plus/
│   │   └── common-webmvc/
│   └── demo/               # 可复用的 DDD 分层服务骨架
│       ├── demo-shared/
│       ├── demo-domain/
│       ├── demo-client/
│       ├── demo-infrastructure/
│       ├── demo-application/
│       ├── demo-adapter/
│       └── demo-starter/   # 启动入口、环境配置、健康检查
├── frontend/               # Vue 3 + Vite + JavaScript 前端基座
│   └── src/
│       ├── api/            # Axios 客户端与接口模块
│       ├── router/         # 路由
│       ├── stores/         # Pinia 状态
│       └── views/          # 页面
├── scenarios/              # 各场景文档与复现笔记，不重复创建前后端工程
│   └── 01-auth/
├── docs/                 # 场景笔记模板
└── README.md
```

## 本地运行

### 启动后端

默认使用 H2 内存数据库，不需要额外安装数据库。服务端口为 `8080`，上下文路径为 `/api`。

```bash
cd backend
mvn clean package
java -jar demo/demo-starter/target/demo-starter-0.0.1-SNAPSHOT.jar
```

验证健康检查：

```bash
curl http://localhost:8080/api/health
```

开发期也可以在 IDE 中直接运行 `com.fullstack.demo.starter.DemoApplication`。

### 启动前端

Vite 开发服务器默认运行在 `http://localhost:5173`，并将 `/api` 请求代理到后端 `http://localhost:8080`。

```bash
cd frontend
npm install
npm run dev
```

打开 [http://localhost:5173](http://localhost:5173)，首页会自动执行后端健康检查。

### 运行测试

```bash
cd backend
mvn test

cd ../frontend
npm run build
```

后端测试使用随机 H2 内存库，不依赖外部数据库；前端构建直接执行 Vite 生产构建。

## 复现规范

每个场景至少回答四个问题：

1. **需求是什么**——真实世界里谁在用它，核心链路是什么；
2. **怎么设计的**——架构图、数据模型、关键技术选型及理由；
3. **怎么实现的**——前后端代码，可运行、可复现的步骤；
4. **踩了什么坑**——与预期不符的地方、压测数据、遗留问题。

## License

MIT（待补充）
