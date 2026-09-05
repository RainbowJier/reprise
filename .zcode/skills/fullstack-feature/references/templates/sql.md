# Schema 与 Migration 参考

仅在已确认目标项目的数据库方言、迁移工具、目录、命名和执行方式后使用。此文件不是可直接执行的 SQL 模板，不能替代项目现有 migration 规范。

## 先确认的事实

| 项目 | 需确认内容 |
|------|------------|
| 存储与方言 | PostgreSQL、MySQL、SQL Server、SQLite、Oracle 或其他；未知时停止 |
| 迁移工具 | Flyway、Liquibase（Spring Boot 生态常见）、Prisma、Alembic、ORM migration 或手工流程 |
| ORM 映射 | Spring Data JPA 实体、MyBatis mapper XML/注解；DDL 与映射必须同步变更 |
| 版本与目录 | 迁移文件命名、顺序、up/down、是否可重放 |
| 线上兼容 | 发布顺序、旧版本兼容窗口、默认值、回填与读写双兼容 |
| 验证与恢复 | dry-run、测试库、schema diff、回滚或备份恢复 |

无版本化迁移工具的项目（功能目录自包含 SQL、启动初始化 SQL、手工 DBA 流程等）：先确认 SQL 落位目录与命名（常见日期前缀）、执行主体、执行顺序与回滚/备份记录方式；脚本要求自包含、幂等、带前置状态检查，并在设计文档与任务中记录执行主体与顺序。执行主体或回滚方式确认不了时保持 `blocked`，不生成可执行 DDL。

## 变更分类

| 变更 | 推荐产物 | 必须说明 |
|------|----------|----------|
| 新建 schema/table/collection | 项目既有的 create migration | 前置版本、关系、索引、执行顺序 |
| 新增或修改字段 | 新的版本化 migration | 默认值、可空性、回填、兼容窗口、回滚 |
| 索引/约束 | 独立或同版本 migration | 数据量、锁表/在线 DDL 风险、验证 |
| 数据回填/修复 | 可审计的数据 migration/job | 批次、幂等、失败恢复、监控 |
| 无数据库 | 不创建脚本 | 在设计和 task 中标记“不适用” |

## 文档骨架

```text
迁移：<按项目数据变更策略落位的文件路径>
目标：<schema/table/model 的变更>
前置状态：<schema 版本，或基准脚本与日期（无版本化工具时）>
兼容策略：<expand/contract、默认值、双读写等>
数据处理：<无 / 回填方案>
验证：<测试数据库、dry-run、schema diff、回归测试>
恢复：<项目已有 down migration / 备份恢复 / 不支持回滚的原因>
```

## 方言示例

只有项目已确认使用 PostgreSQL 时，才可采用 `COMMENT ON`、部分索引等 PostgreSQL 语法；其他方言使用项目既有 migration 示例。不要混用 SQL Server 方括号、MySQL 反引号、Oracle 语法或其他方言。
