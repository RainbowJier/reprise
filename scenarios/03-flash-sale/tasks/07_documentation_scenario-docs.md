---
id: flash-sale_07_documentation_scenario_docs
name: "场景文档收尾（SVG 配图 + NOTES.md + README 索引）"
type: documentation
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_05_test_flash_concurrency, flash-sale_06_ui_flash_page]
profiles: [documentation]
files:
  - scenarios/03-flash-sale/diagrams/flash-sale-architecture.svg
  - scenarios/03-flash-sale/diagrams/oversell-guard.svg
  - scenarios/03-flash-sale/NOTES.md
  - README.md
---

# 场景文档收尾（SVG 配图 + NOTES.md + README 索引）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `documentation` |
| 已启用 profile | documentation |
| 架构边界 | scenarios/03-flash-sale 文档目录（design.md 已在阶段一产出）；README 场景索引 |
| 结构分析 | 场景 01 模式：design.md 引用相对路径 SVG（DocView 经 scenarioDocs.js 的 assets 映射渲染）；NOTES.md 按「需求/设计取舍/实现要点/踩坑/验证数据/延伸」六段 |
| 完成条件 | 前端 `npm run build` 通过（SVG import 解析成功）；README 索引更新为已完成 |

## 需求与验收

- 用户目标：两张 SVG 配图（请求漏斗架构图、防超卖三层防线图，NPG 学术配色，与场景 01 配图风格一致）；NOTES.md 复现笔记；README 场景索引 03 置 ✅ 并链接 NOTES。
- 验收：`npm run build` 通过；DocView 打开 /scenario/03-flash-sale/doc 图片正常渲染。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 配图 | 用工作区 research-svg skill 生成：①flash-sale-architecture.svg（请求漏斗分层架构）②oversell-guard.svg（check-then-act 竞态 vs 原子条件更新对比）；文件名与 design.md 引用、scenarioDocs.js 映射三方一致 |
| NOTES | 验证数据（并发测试/冒烟实际结果）以任务 05 执行记录为准，不预先编造 |
| 依赖 | 05（验证数据）、06（文档路由可达） |

## 实现步骤

1. research-svg 生成两张 SVG 至 scenarios/03-flash-sale/diagrams/；
2. 撰写 NOTES.md（含真实执行数据）；
3. README 场景索引 03 行置 ✅ 并链接 NOTES.md；
4. `npm run build` 验证。

## 涉及文件

| 文件 | 模块/区域 | 操作 | 说明 |
|------|-----------|------|------|
| `scenarios/03-flash-sale/diagrams/flash-sale-architecture.svg` | 场景目录 | 新增 | 架构图 |
| `scenarios/03-flash-sale/diagrams/oversell-guard.svg` | 场景目录 | 新增 | 防超卖原理图 |
| `scenarios/03-flash-sale/NOTES.md` | 场景目录 | 新增 | 复现笔记 |
| `README.md` | 仓库根 | 修改 | 场景索引 03 状态与链接 |

## 完整代码（供手动敲写）

NOTES.md 与 README 修改无固定代码模板：NOTES 按六段结构撰写、内容取自实际执行记录；README 仅改索引表 03 行（状态 ✅ 已完成 + NOTES 链接）。SVG 由 research-svg skill 生成，无手写代码。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 通过（含 03 场景 md/SVG 资源打包） |
| manual | 登录后访问 /scenario/03-flash-sale/doc | design.md 渲染正常，两张图可见 |

## 风险与阻塞

- 风险：无。
- 阻塞：无。
- 执行记录：2026-09-07 research-svg 生成两张 SVG（820px NPG 配色，XML 校验通过）；NOTES.md 以实际验证数据落盘（mvn 20/20、冒烟 7/7、并发不变式）；README 索引 03 置 ✅ 并链接 NOTES；AGENTS.md 认证注意事项同步 /flash/* 与秒杀错误码分段；design.md 修正「防超杀」笔误。`npm run build` 复验通过。
