# 微书包 - 智能书库前端

一个基于 Vue 3 + Vite + TypeScript 的全新前端实现，服务于 WSB-SmartLibrary 项目的图书管理、借阅协作、社区互动和 AI 阅读能力。

## 技术栈

- Vue 3 + Composition API
- Vite
- TypeScript
- Pinia
- Vue Router
- Axios

## 本地开发

```bash
npm install
npm run dev
```

默认开发地址为 [http://localhost:5173](http://localhost:5173)，并通过 Vite 代理将 `/api` 请求转发到 `http://localhost:8080`。

## 打包

```bash
npm run build
npm run preview
```

## 目录说明

- `src/api`：后端接口封装
- `src/components`：公共组件
- `src/layouts`：布局容器
- `src/router`：路由与守卫
- `src/stores`：Pinia 状态管理
- `src/utils`：请求、认证、消息与格式化工具
- `src/views`：页面视图
