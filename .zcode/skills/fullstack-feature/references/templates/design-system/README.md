# 前端设计系统参考资料

> 本目录由 fullstack-feature 根据目标项目事实生成，供 AI agent / 开发者在编写前端代码前参考。
> **它不是运行时依赖，目标项目代码不得从本目录 import 或 @import 任何内容。**

## 参考与代码的边界

- 参考资料：`design-system.md`、`rules.md`、`tokens.md` 和 `ui/`；
- 目标项目代码：位于项目自身的组件、页面和样式目录；
- 接入方式：参考 `ui/` 和文档，在目标项目内 copy & own；
- 本目录生成时不修改业务代码、依赖、构建配置或路径别名。

## 本目录结构

```text
$FRONTEND_ROOT/design-system/
├── README.md
├── design-system.md
├── rules.md
├── tokens.md
└── ui/
    └── README.md
```

## 使用方式

1. 开发前阅读 `design-system.md`，先查已有组件；
2. 阅读 `rules.md`，确认组件、样式和 Token 约束；
3. 阅读 `tokens.md`，复用目标项目已有命名；
4. 需要实现时，在目标项目自己的组件目录中完成，不从本目录引入；
5. 目标项目的规范与相邻代码优先于本参考资料。

## 维护方式

本目录是项目事实的参考快照。目标项目组件、样式或依赖发生变化时，增量更新对应文档；没有事实依据的内容标记为“未识别”，不要猜测或大规模改造业务代码。
