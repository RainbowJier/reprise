# 设计系统 · 组件清单

> 本文件由 fullstack-feature 根据目标项目现状生成，供 AI agent / 开发者参考。
> `design-system/` 不是运行时依赖；目标项目代码不得从本目录 import。

## 项目事实

- 前端根目录：`$FRONTEND_ROOT`
- 技术栈：{frontend_stack}
- 组件来源：{component_source}
- 组件总数：{component_count}

## 使用原则

- 先查本清单和目标项目自身组件目录，再决定是否需要新增组件；
- 源码路径只记录已在目标项目中确认存在的文件；
- 外部 UI 库只记录项目实际使用的组件和封装方式，不复制第三方源码；
- 如果没有已确认组件，保留“当前未识别组件”的说明，不凭空补齐组件。

## 组件总表

| 组件 | 用途 | 源码路径 | 来源 | 状态 |
| --- | --- | --- | --- | --- |
| {component_inventory_rows} |  |  |  |  |

## 组件条目固定格式

### {component_name}

- **源码**：`{component_source_path}`
- **来源**：{component_origin}
- **用途**：{component_purpose}
- **Props**：{component_props}
- **Emits**：{component_emits}
- **Slots**：{component_slots}
- **何时用**：{component_when_to_use}
- **何时不用**：{component_when_not_to_use}
- **相似组件辨析**：{component_distinction}

## 未识别组件

{unrecognized_components}

## 更新规则

组件实现、Props、Emits、Slots 或使用约定发生变化时，同步更新本文件。没有项目事实支持时不要填写猜测内容。
