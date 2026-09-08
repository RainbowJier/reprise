# reprise 前端

基于 Vue 3 + Vite + JavaScript 的前端基座，使用 ES Modules，不使用 TypeScript。

## 技术栈

- Vue 3
- Vite
- JavaScript（ES Modules）
- vue-router
- Pinia
- Axios

## 本地开发

```bash
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`，`/api` 请求会代理到 Spring Boot 后端 `http://localhost:8080`。

## 构建生产包

```bash
npm run build
```

构建产物位于 `dist/` 目录。

## 场景知识库界面

- 登录后首页为场景总览，支持关键词搜索与「全部 / 可体验 / 规划中」筛选。
- 每个场景包含技术文档与交互演示，可通过侧栏或页内标签切换。
- 技术文档自动生成章节目录，提供阅读进度、代码复制和窄屏表格滚动。
- 手机端使用可关闭、支持 Escape 和焦点约束的导航抽屉。
- 主题为纸感浅绿，语义 token 在 `src/style.css`，通用组件在 `src/components/`。
- 设计与验收记录见 [REDESIGN.md](./REDESIGN.md)。

文档渲染回归测试（无需新增依赖）：

```bash
node --test src/utils/renderDoc.test.js
```

生产预览使用现有 CORS 白名单端口，避免登录 POST 被拒绝：

```bash
npm run preview -- --port 5174 --strictPort
```
