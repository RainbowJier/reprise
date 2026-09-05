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
