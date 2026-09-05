# reprise

> reprise /rəˈpriːz/ · 名词 · 重演，复现

**复现经典前后端场景的实践记录** · Full-stack reproductions of classic web scenarios

每个场景独立成篇：从需求出发，亲手实现一遍后端与前端，记录设计取舍与踩过的坑。

## 场景索引

| #   | 场景                       | 目录                                       | 状态  | 笔记 |
| --- | -------------------------- | ------------------------------------------ | ----- | ---- |
| 01  | 用户登录与认证（JWT/SSO）  | `scenarios/01-auth`                        | ☐ 规划 | —    |
| 02  | 短链接服务                 | `scenarios/02-short-link`                  | ☐ 规划 | —    |
| 03  | 秒杀 / 高并发抢购          | `scenarios/03-flash-sale`                  | ☐ 规划 | —    |
| 04  | Feed 流（推 / 拉 / 推拉结合） | `scenarios/04-feed`                     | ☐ 规划 | —    |
| 05  | 即时通讯（WebSocket）      | `scenarios/05-im`                          | ☐ 规划 | —    |
| 06  | 文件上传（分片 / 秒传）    | `scenarios/06-upload`                      | ☐ 规划 | —    |
| 07  | 支付对接与回调对账         | `scenarios/07-payment`                     | ☐ 规划 | —    |
| 08  | RBAC 权限系统              | `scenarios/08-rbac`                        | ☐ 规划 | —    |
| 09  | 分布式锁与库存扣减         | `scenarios/09-distributed-lock`            | ☐ 规划 | —    |
| 10  | 多级缓存与一致性           | `scenarios/10-caching`                     | ☐ 规划 | —    |

> 状态：☐ 规划 → 🚧 进行中 → ✅ 已完成。目录按需创建，不必一次建齐。

## 目录结构

```
reprise/
├── scenarios/          # 每个场景一个子目录
│   └── 01-auth/
│       ├── backend/    # 后端实现
│       ├── frontend/   # 前端实现
│       ├── README.md   # 场景说明：需求、架构、运行方式
│       └── NOTES.md    # 复现笔记：难点、取舍、踩坑
├── docs/
│   └── TEMPLATE.md     # 场景笔记模板
└── README.md
```

## 复现规范

每个场景至少回答四个问题：

1. **需求是什么**——真实世界里谁在用它，核心链路是什么；
2. **怎么设计的**——架构图、数据模型、关键技术选型及理由；
3. **怎么实现的**——前后端代码，可运行、可复现的步骤；
4. **踩了什么坑**——与预期不符的地方、压测数据、遗留问题。

## License

MIT（待补充）
