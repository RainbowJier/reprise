# 设计系统 · 组件清单

> 本文件由 fullstack-feature 根据目标项目现状生成，供 AI agent / 开发者参考。
> `design-system/` 不是运行时依赖；目标项目代码不得从本目录 import。

## 项目事实

- 前端根目录：`frontend/`
- 技术栈：Vue 3.5（`<script setup>` 组合式 API）+ Vite 8 + JavaScript ESM；vue-router 4、Pinia、axios；**样式体系已决策采用 Tailwind CSS v4（utility-first + `@theme`，接入随场景 01 落地）**；无 UI 组件库、无 TypeScript
- 组件来源：自有（AdminLayout 布局组件，随 RuoYi 风格管理端改造引入）
- 组件总数：1

## 使用原则

- 先查本清单和目标项目自身组件目录，再决定是否需要新增组件；
- 源码路径只记录已在目标项目中确认存在的文件；
- 外部 UI 库只记录项目实际使用的组件和封装方式，不复制第三方源码；
- 如果没有已确认组件，保留"当前未识别组件"的说明，不凭空补齐组件。

## 组件总表

| 组件 | 用途 | 源码路径 | 来源 | 状态 |
| --- | --- | --- | --- | --- |
| AdminLayout | 管理端页面骨架（侧栏场景菜单 + 顶栏用户区 + 内容区） | `frontend/src/layouts/AdminLayout.vue` | 自有 | 已确认 |

### AdminLayout

- **源码**：`frontend/src/layouts/AdminLayout.vue`
- **来源**：自有（RuoYi 风格管理端布局，任务 auth-jwt_11）
- **用途**：登录后的管理端页面骨架——左侧深色侧栏渲染 `config/scenarios.js` 场景二级菜单（父项手风琴展开，子项「详情页效果展示」「技术说明文档」；未实现场景置灰「规划中」），右侧顶栏含三级面包屑与用户下拉（登出），内容区为 `<RouterView />`
- **Props**：无
- **Emits**：无
- **Slots**：default（经子路由 RouterView 承载）
- **何时用**：所有 `requiresAuth` 的管理端路由，作为布局父路由组件
- **何时不用**：登录/注册等独立全屏页
- **相似组件辨析**：无等价组件；它是路由布局而非可复用业务组件

## 组件条目固定格式

### {component_name}

- **源码**：`{component_source_path}`
- **来源**：{component_origin}
- **用途**：{component_purpose}
- **Props**：{component_props}
- **Emits**：{component_emits}
- **Slots**：{component_slots}
- **何时用**：{component_when_to_use}
- **何时不用**：{component_when_not_use}
- **相似组件辨析**：{component_distinction}

> 以上为后续登记组件时的固定格式，当前无组件条目。

## 未识别组件

项目现有页面 `frontend/src/views/HomeView.vue` 直接内联使用全局样式类（`.card` / `.row` 等，定义于 `frontend/src/style.css`），未抽象可复用组件。Tailwind 接入任务会将这些存量类改写为 utilities；此后样式复用即 utilities 复用，出现重复的业务语义组合时在 `frontend/src/components/` 新建 Vue SFC 并回填本清单。

## 更新规则

组件实现、Props、Emits、Slots 或使用约定发生变化时，同步更新本文件。没有项目事实支持时不要填写猜测内容。
