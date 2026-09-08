# reprise · 前端设计系统

> 基于当前前端实现重新生成，供开发者和 AI agent 在扩展场景时参考。视觉方向：**纸感浅色、石墨文字、松绿强调的工程场景知识库**。
>
> **本目录不是运行时依赖。禁止从中 import、@import 或建立运行时路径别名。**

## 这套资料解决什么问题

让后续页面与现有的场景集合、技术文档、交互演示保持一致。先识别已经存在的组件，再复用页面内的样式组合；不把一组重复 utilities 虚构成已经封装好的组件库。

本次由用户明确要求基于当前源码重新生成已有目录，因此更新旧版参考文件并补充 UI 说明；不修改运行时代码、依赖或构建配置。

## 阅读顺序

| 文档 | 内容 |
| --- | --- |
| [design-system.md](./design-system.md) | 设计方向、真实组件清单、接口与场景接入规则 |
| [tokens.md](./tokens.md) | 主题颜色、字体、间距、断点、圆角、层级和动效 |
| [rules.md](./rules.md) | 后续开发必须遵守的复用、样式、交互及文档边界 |
| [ui/README.md](./ui/README.md) | UI 参考索引与使用方法 |
| [ui/layouts.md](./ui/layouts.md) | 总体框架、首页、文档、演示、认证页的响应式布局 |
| [ui/patterns.md](./ui/patterns.md) | 按钮、表单、卡片、标签、代码区等样式配方 |
| [ui/interactions.md](./ui/interactions.md) | 导航、请求反馈、账号、阅读与演示交互，含可访问性边界 |
| [sources.json](./sources.json) | 本次探测的源码文件及 SHA-256 校验值，用于识别参考资料是否过期 |

## 项目事实

- 前端根目录：`frontend/`。
- Vue 3、JavaScript ESM、`<script setup>`、Vite 8。
- Tailwind CSS v4，通过 `@tailwindcss/vite` 接入，Typography 用于文档正文。
- Vue Router、Pinia、Axios、Marked；无外部 UI 组件库。
- 3 个公共组件：`AppIcon`、`AuthShell`、`AdminLayout`。其中 2 个位于 `src/components/`，1 个为路由布局。
- 当前收录 10 个场景元信息，2 个场景提供实际文档与演示。
- 仅有浅色主题；没有暗色模式、主题切换或远程字体加载实现。

## 权威来源与冲突处理

1. 用户当前明确需求与仓库规范。
2. 当前源码、构建配置及相邻实现。
3. 本设计系统参考资料。

主题值来自 [src/style.css](../src/style.css)，不是从截图反推。布局和交互来自页面源码，不是从品牌文案推断。早期 RuoYi 深色侧栏描述与当前实现不一致时，不能将旧外观写成当前事实；业务路由、认证约束仍按现行契约保留。

文档中使用以下标记区分状态：

- **已有事实**：可以在源码中定位。
- **约定**：后续开发应保持的实践，并不代表所有存量控件已经具备。
- **未实现 / 未验证**：不能对用户宣称存在或已经验收的能力。

## 使用边界

- 复用现有组件时从 `@/components/` 等源码路径引入，不从本目录复制一个同名组件。
- `ui/` 只存 Markdown 参考说明和样式片段，没有可执行的 Vue 组件副本或 Storybook。
- 样式片段按 copy & own 方式适配到业务组件，变量和事件由目标页面提供。
- 不为生成参考资料而新增主题、UI 框架、npm scripts 或运行时抽象。
- 历史改版记录在 [REDESIGN.md](../REDESIGN.md)；本目录描述当前状态，不充当功能任务历史。

## 更新与验证

变更组件接口时同步组件清单；变更 `@theme` 同步 Token 表；变更响应式或状态处理同步 UI 文档。**生成日期不作为同步依据**，应对照源码差异与 `sources.json`。

在仓库根目录可运行已有验证命令：

```bash
npm --prefix frontend run build
node --test frontend/src/utils/renderDoc.test.js
git diff --check -- frontend/design-system
```

`package.json` 只有 `dev`、`build`、`preview` scripts，没有 `lint`、`test`、`typecheck` script；已有 Node 原生测试文件不能等同于没有前端测试。

本次重生成采用源码审阅、路径/Token/哈希一致性检查。没有修改页面，未重新执行浏览器视觉验收，也不以文档检查宣称全站可访问性合规。
