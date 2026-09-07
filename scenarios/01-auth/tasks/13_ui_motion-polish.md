---
id: auth-jwt_13_ui_motion_polish
name: "前端动效与交互细节优化"
type: ui
subtype: null
status: completed
blocked_reason: null
depends: [auth-jwt_12_ui_scenario_doc_menu]
profiles: [frontend]
files:
  - frontend/src/style.css
  - frontend/src/layouts/AdminLayout.vue
  - frontend/src/views/LoginView.vue
  - frontend/src/views/RegisterView.vue
  - frontend/design-system/tokens.md
---

# 前端动效与交互细节优化

## 任务范围

| 项目 | 内容 |
|------|------|
| 类型 | `ui`（增量：动效与交互打磨） |
| 已启用 profile | frontend |
| 架构边界 | 全局样式（动效 token/过渡类）+ AdminLayout + 登录注册页 |
| 结构分析 | 现状：菜单展开/下拉/路由切换均瞬时切替，按钮无微交互；动效 token 在 design-system 中为「未识别」 |
| 完成条件 | `npm run build` 绿；浏览器验证过渡生效、交互闭环无回归 |

## 需求与验收

- 包含：子菜单高度动画、菜单项颜色过渡、路由切换过渡、下拉弹层动画+路由收起、按钮按压微交互与加载 spinner、错误 shake、卡片入场动画、reduced-motion 降级。
- 不包含：路由级骨架屏、列表 stagger、主题切换动画。
- 验收：展开/收起/切页/下拉均有平滑过渡；登录加载见 spinner；重复错误 shake 重触发；`npm run build` 通过。

## 设计与依赖

| 类别 | 事实 / 决策 |
|------|-------------|
| 动效 token | `--animate-shake` / `--animate-fade-up`（Tailwind v4 @theme 内嵌 keyframes）；页面/弹层过渡类（.page/.pop）落在 style.css 全局 |
| 交互修正 | 用户下拉在路由变化时自动收起（watch route.path） |
| 无障碍 | `prefers-reduced-motion: reduce` 时全局动画/过渡时长降为 0.01ms |
| 依赖 | 12（布局与文档页，已完成） |

## 实现步骤

1. style.css：动效 token + keyframes、按钮 base 层过渡、.page/.pop 过渡类、reduced-motion；
2. AdminLayout：grid-rows 展开动画、颜色过渡、下拉 Transition + origin + 路由收起、RouterView 路由过渡；
3. Login/Register：animate-fade-up 入场、spinner、错误 :key + animate-shake；
4. design-system tokens 同步；build + 浏览器验证。

## 完整代码（供手动敲写）

### frontend/src/style.css（修改，摘要）

```css
@theme {
  /* …既有 token… */
  --animate-shake: shake 0.4s ease-in-out;
  --animate-fade-up: fade-up 0.35s ease-out both;

  @keyframes shake {
    10%, 90% { transform: translateX(-1px); }
    20%, 80% { transform: translateX(2px); }
    30%, 50%, 70% { transform: translateX(-3px); }
    40%, 60% { transform: translateX(3px); }
  }
  @keyframes fade-up {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: none; }
  }
}

@layer base {
  body { @apply bg-canvas font-sans text-ink antialiased; }

  button { @apply transition-all duration-150 active:scale-[0.97]; }

  @media (prefers-reduced-motion: reduce) {
    *, ::before, ::after {
      animation-duration: 0.01ms !important;
      transition-duration: 0.01ms !important;
    }
  }
}

/* 路由切换过渡（RuoYi fade-transform 风格） */
.page-enter-active, .page-leave-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.page-enter-from { opacity: 0; transform: translateX(12px); }
.page-leave-to { opacity: 0; transform: translateX(-12px); }

/* 弹层过渡（用户下拉等） */
.pop-enter-active, .pop-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: scale(0.95) translateY(-4px); }
```

### frontend/src/layouts/AdminLayout.vue（修改，关键段）

- 子菜单容器改为 grid 高度动画：外层 `grid transition-[grid-template-rows] duration-200 ease-in-out`，展开 `grid-rows-[1fr]`、收起 `grid-rows-[0fr]`，内层 `overflow-hidden` 承载两个子菜单链接；
- 父菜单按钮与子菜单链接补 `transition-colors`；
- 用户下拉外包 `<Transition name="pop">`，面板加 `origin-top-right`；`watch(() => route.path, () => (userMenuOpen.value = false))`；
- 内容区改 RouterView 插槽过渡：`<RouterView v-slot="{ Component }"><Transition name="page" mode="out-in"><component :is="Component" :key="route.path" /></Transition></RouterView>`。

### frontend/src/views/LoginView.vue / RegisterView.vue（修改，关键段）

- 外层卡片加 `animate-fade-up`；
- 提交按钮加载态加 spinner：`<span v-if="loading" class="mr-1 inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />`；
- 错误提示重触发动画：`<p v-if="error" :key="error" class="text-[13px] text-danger animate-shake">`。

## 验证

| 验证方式 | 命令或步骤 | 预期结果 |
|----------|------------|----------|
| build | `cd frontend && npm run build` | 构建成功 |
| manual | 展开收起子菜单 / 切换详情↔文档 / 打开用户下拉 | 均有平滑过渡，路由变化下拉收起 |
| manual | 登录：加载 spinner、错误 shake（连续两次错误重触发） | 正常 |
| manual | 系统开启"减弱动态效果"后 | 过渡基本瞬时（降级生效） |

## 风险与阻塞

- 风险：grid-rows 动画需现代浏览器（Chrome 107+），目标环境满足；路由过渡与 keep-alive 无叠加（无 keep-alive）。
- 阻塞：无。
- 执行记录：2026-09-05 落盘全部动效（shake/fade-up token、grid-rows 子菜单展开、按钮微交互、spinner、下拉 pop 动画 + 路由变化自动收起、reduced-motion 降级）；`npm run build` 绿；样式注入与交互闭环逐项验证通过。

修订：1 - 路由切换过渡两度调整：最初 `<Transition name="page" mode="out-in">` 在切换被 HMR/快速点击打断时卡死在离场（main 残留旧页），加 `:duration` 兜底后变为切换后 main 空（懒加载视图 + out-in 状态机在该组合下 enter 不可靠，无任何控制台报错）。最终方案：**移除组件级 Transition**，视图根元素自带 `animate-fade-up` 入场动画（挂载即播放，无可卡状态），style.css 删除 `.page-*` 类。终验：切页渲染正常、入场动画生效、90ms 中途连击打断后内容完整。教训记入 NOTES 候选。
