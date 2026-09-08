---
id: flash-sale_10_documentation_design_doc_split
name: "设计文档拆分（flash-sale.md 补齐 + design.md 恢复纯讲义）"
type: documentation
subtype: null
status: completed
blocked_reason: null
depends: [flash-sale_07_documentation_scenario_docs, flash-sale_09_ui_race_console]
profiles: [documentation]
files:
  - scenarios/03-flash-sale/flash-sale.md
  - scenarios/03-flash-sale/design.md
  - scenarios/01-auth/auth-jwt.md
  - scenarios/01-auth/design.md
  - AGENTS.md
---

# 设计文档拆分（flash-sale.md 补齐 + design.md 恢复纯讲义）

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `documentation`（增量：文档约定变更，用户决策） |
| 背景决策 | ① 删除 NOTES.md（踩坑/实测数据不单独成文件）；② fullstack-feature 阶一「设计文档」与前端「技术文档」页展示的讲义**分离为两份**——设计文档含项目实现细节与踩坑实测，design.md 为不含项目内容的纯讲义（对齐场景 01 `auth-jwt.md` 既有模式） |
| 完成条件 | 两场景双文档结构一致；design.md 无项目相关内容；`npm run build` 通过 |

## 需求与验收

- 补齐 `scenarios/03-flash-sale/flash-sale.md`（skill 阶一设计文档：需求概述/头脑风暴取舍/数据模型/业务流程/影响范围/冻结契约/模块拆分/非功能/验证建议/踩坑与实测）。
- 两份 design.md 恢复纯讲义：移除「踩坑与实测」节（内容分别迁入 flash-sale.md 十节、auth-jwt.md 十节），导语与章节号复原。
- AGENTS.md 固化双文档约定，后续场景照此执行。

## 涉及文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `scenarios/03-flash-sale/flash-sale.md` | 新增 | 设计文档（slug 命名，对齐 auth-jwt.md） |
| `scenarios/03-flash-sale/design.md` | 修改 | 移除踩坑与实测，恢复九节纯讲义 |
| `scenarios/01-auth/auth-jwt.md` | 修改 | 追加「十、踩坑与实测」；两处引用由 design.md 改指本文档 |
| `scenarios/01-auth/design.md` | 修改 | 移除踩坑与实测，恢复十节纯讲义 |
| `AGENTS.md` | 修改 | scenarios/ 条目固化为双文档分工约定 |

## 验证

| 验证方式 | 命令或步骤 | 结果 |
|----------|------------|------|
| build | `cd frontend && npm run build` | 通过（design.md `?raw` 打包内容变化不影响渲染契约） |
| manual | grep design.md 无项目关键词（mvn/H2/IDEA/FLASH_BASE 等） | 纯讲义复归 |

## 风险与阻塞

- 阻塞：无。
- 执行记录：2026-09-07 完成。前一轮曾把踩坑实测迁入 design.md（当时定位为「design.md 承载踩坑」），本轮按用户最新决策再迁至设计文档并固化约定——NOTES 删除→design 承载→设计文档承载，两跳均无内容丢失。
