# UI 参考说明

本目录保存**当前 UI 的布局说明、utilities 配方和交互边界**，不是运行时组件包，也不复制业务 SFC。

## 索引

| 文档 | 回答的问题 | 主要来源 |
| --- | --- | --- |
| [layouts.md](./layouts.md) | 页面如何排列，何时切换列数和导航方式 | AdminLayout、HomeView、AuthShell、DocView |
| [patterns.md](./patterns.md) | 按钮、卡片、表单、徽标、代码区如何组合 | 当前组件和页面模板 |
| [interactions.md](./interactions.md) | 如何响应搜索、请求、导航、复制和实验操作 | 当前脚本、路由与 store |

公共组件接口见 [design-system.md](../design-system.md)，设计变量见 [tokens.md](../tokens.md)。

## 如何使用

1. 优先引用已有 `AppIcon`、`AuthShell`，场景页面通过路由复用 `AdminLayout`。
2. 没有现成组件的视觉模式按配方组合 utilities，事件与状态使用目标页面真实逻辑。
3. 示例是参考片段，不是新增的 Button/Card/Toast API；不能从文档 import。
4. 不为参考区创建占位组件或拷贝第三方依赖源码。
5. 涉及接口、业务或可访问性时继续读来源文件，不把静态示例当完整实现。

## 当前不提供

- 可运行的 Storybook 或 HTML 组件展台。
- Vue 源码镜像、主题 npm 包、全局 BEM 样式。
- 未经测量的 WCAG 合规声明。
- 暗色主题、语法高亮或通用对话框等尚未实现的能力。

本次是文档重生成；因此没有额外制作 UI 或新增运行时依赖。
