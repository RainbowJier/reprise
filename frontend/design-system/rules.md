# 设计系统 · AI agent 参考规则

> 本文件用于指导 AI agent / 开发者，不是前端运行时配置。
> 目标项目代码不得从 `frontend/design-system/` import 或 @import。

## R0 参考与代码边界

- `design-system/` 只存参考资料，不是组件包，不是 npm 依赖；
- `design-system/ui/` 是参考实现或参考说明；目标项目使用时必须复制到项目自己的组件目录后再适配；
- 不建立指向 `design-system/` 的路径别名、包依赖或运行时引入；
- 目标项目已有规范、相邻代码和实际构建配置优先于本文件。

## R1 组件复用

- 项目样式体系为 Tailwind CSS v4 utility-first（用户决策），无 UI 组件库、无自有组件（当前 0 个）；
- 样式复用即 utilities 复用：优先组合既有 token utilities（见 [tokens.md](./tokens.md)）；
- 同一组 utilities 在 ≥2 处重复出现且承载业务语义时，沉淀为 `frontend/src/components/` 下的 Vue SFC 组件，而不是 `@apply` 全局类；
- 引入 UI 组件库（Element Plus 等）需用户确认，不得擅自添加。

## R2 Token 与样式

- 颜色、字体、圆角只从 [tokens.md](./tokens.md) 登记的 `@theme` token 生成 utilities，禁止在 utilities 里硬编码 hex（`bg-[#3370ff]` 这类任意值仅限一次性对齐，不留存）；
- 间距/字号/宽度用 Tailwind 默认刻度，语义色用自定义 token；
- `src/style.css` 只保留 Tailwind 入口（`@import "tailwindcss"` + `@theme` 块）与必要的 base 层样式，不再新增手写 BEM 全局类；
- 既有存量全局类随 Tailwind 接入任务一次性改写为 utilities，避免双体系并存。

## R3 文档同步

- 新增/修改组件时同步更新 `design-system.md` 组件清单；
- `@theme` token 或映射变化时同步更新 `tokens.md`；
- 规则变化时同步更新本文件；
- 文档只能记录已从项目读取到的事实，未知内容明确标记为 `unknown` 或"未识别"。

## R4 验证边界

- design-system 生成后确认目标前端源码没有新增指向本目录的 import；
- 不因为生成参考资料而修改业务代码、构建配置或依赖（Tailwind 属于用户决策的工程变更，由开发任务执行）；
- 后续功能开发仍使用目标项目自己的组件、样式和验证命令（`npm run build`；无 lint/test 配置；Tailwind v4 经 `@tailwindcss/vite` 参与构建校验）。
