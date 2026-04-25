# WSB SmartLibrary

一个面向个人藏书管理、群组协作借阅与 AI 阅读辅助的全栈智能书库项目。

项目的重点不只是“图书管理系统”，而是把个人书架、社区借阅流程与 RAG 检索能力串成一条完整业务链路，让“图书”既能被管理，也能被语义理解、相似召回和自然语言检索。

本仓库采用前后端一体的 monorepo 结构：

- 后端：Spring Boot 3 + Spring Cloud Alibaba 微服务
- 前端：Vue 3 + Vite + TypeScript
- AI 检索：Spring AI + PostgreSQL + pgvector + `zhparser`
- 本地依赖：MySQL、Redis、Nacos、RabbitMQ、PostgreSQL


## 项目亮点

- 不是通用聊天机器人，而是围绕“个人藏书 / 群组借阅 / 阅读场景”落地的垂直 RAG
- 不只是单人书架管理，还支持群组协作借阅、审批、归还和统计分析
- 既支持基于书名、作者、关键词的普通检索，也支持“想看一本讲女性成长的现实主义小说”这类自然语言推荐
- 不是只做向量库演示，而是完整实现了“摘要生成 -> 向量化 -> 混合检索 -> 结果重排 -> 图书回填”的业务闭环
- 中文检索不是简单 `LIKE`，而是结合 `pgvector` 向量召回、`zhparser` 中文全文检索和 RRF 重排
- 推荐结果可以限定在“我的藏书”范围内，适合展示个性化私有书库场景

## 核心能力

- 用户注册、登录、资料维护、短信验证码
- 图书录入、编辑、删除、ISBN 查询、封面上传
- 书架管理、上架下架、阅读记录、借入借出管理
- 图书收藏、群组管理、社区借阅申请与审批
- 个人统计、借阅统计、收藏统计、排行榜
- AI 摘要、书评聚合、相似图书推荐、自然语言推荐

## 典型业务闭环

- 图书录入闭环：新增图书 -> 生成 AI 摘要 -> 写入向量库 -> 参与自然语言推荐
- 社区借阅闭环：群组内公开书架 -> 发起借阅申请 -> 审批通过 -> 生成借阅记录 -> 归还回写状态
- 统计分析闭环：图书、借阅、收藏等行为沉淀到个人统计和排行榜

## 系统架构

```mermaid
flowchart LR
    A["Vue 3 Frontend"] --> B["wsb-gateway"]

    B --> C["wsb-user"]
    B --> D["wsb-book"]
    B --> E["wsb-community"]
    B --> F["wsb-file"]
    B --> G["wsb-rag"]

    B --> H["Nacos"]
    C --> H
    D --> H
    E --> H
    F --> H
    G --> H

    C --> I["MySQL"]
    D --> I
    E --> I
    F --> I

    C --> J["Redis"]
    D --> J
    E --> J
    G --> J

    G --> K["RabbitMQ"]
    G --> L["PostgreSQL + pgvector + zhparser"]
```

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端基础 | Java 17, Maven, Spring Boot 3.2.4 |
| 微服务 | Spring Cloud 2023, Spring Cloud Alibaba, OpenFeign, Nacos |
| 数据访问 | MyBatis-Plus, MySQL 8 |
| 权限认证 | Sa-Token |
| 文档 | OpenAPI 3, Knife4j |
| 缓存与消息 | Redis 7, RabbitMQ |
| AI / 检索 | Spring AI, pgvector, PostgreSQL 16, zhparser |
| 前端 | Vue 3, Vite, TypeScript, Pinia, Vue Router, Axios, ECharts |

## 仓库结构

```text
.
├─ frontend/                 # Vue 3 前端
├─ docker/                   # 本地依赖环境与 pgvector 镜像构建
├─ docs/                     # 项目文档
├─ sql/                      # 建表、迁移、RAG 初始化脚本
├─ wsb-gateway/              # 网关服务
├─ wsb-common/               # 公共基础模块
├─ wsb-api/                  # Feign API 契约模块
└─ wsb-modules/              # 业务模块集合
   ├─ wsb-user/              # 用户域
   ├─ wsb-book/              # 图书域
   ├─ wsb-community/         # 社区域
   ├─ wsb-file/              # 文件域
   └─ wsb-rag/               # 智能检索 / AI 推荐域
```

## 模块说明

| 模块 | 说明 |
| --- | --- |
| `wsb-gateway` | 统一入口、路由转发、接口文档聚合 |
| `wsb-user` | 注册登录、用户信息、内部昵称查询 |
| `wsb-book` | 图书、书架、借阅、阅读记录、收藏、ISBN 查询 |
| `wsb-community` | 群组、成员、社区借阅申请、审批流、统计分析 |
| `wsb-file` | 图片上传与读取 |
| `wsb-rag` | AI 摘要、书评聚合、相似图书、智能推荐 |
| `wsb-api-*` | 各服务间的 DTO / VO / Feign 契约 |
| `wsb-common-*` | 认证、日志、文档、MyBatis、核心工具类 |

更细的业务边界可以查看 [docs/architecture/modules.md](./docs/architecture/modules.md)。

## 前端页面覆盖

当前前端已经覆盖这些主要页面：

- 登录 / 注册
- 首页工作台
- 我的图书
- 图书详情
- 借阅管理
- 我的收藏
- 社区协作
- 数据统计
- 个人中心

前端路由与接口封装位于：

- [frontend/src/router/index.ts](./frontend/src/router/index.ts)
- [frontend/src/api](./frontend/src/api)

## 本地开发环境

### 1. 环境要求

| 组件 | 建议版本 |
| --- | --- |
| JDK | 17 |
| Maven | 3.9+ |
| Node.js | 20+ |
| npm | 10+ |
| Docker Desktop | 最新稳定版 |

### 2. 启动基础依赖

仓库已经提供统一的本地依赖编排文件：

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

默认会启动：

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| MySQL | `3306` | 业务库 + Nacos 配置库 |
| Redis | `6379` | 缓存、会话、短期数据 |
| Nacos | `8848`, `9848` | 注册中心 + 配置中心 |
| RabbitMQ | `5672`, `15672` | RAG 异步任务 |
| PostgreSQL / pgvector | `5432` | 向量存储与中文全文检索 |

说明：

- 当前 `docker/docker-compose.yml` 默认偏向 Win11 本地开发环境。
- 默认数据目录统一使用 `E:/Docker/...`。
- 如果你的磁盘路径不同，可以通过环境变量覆盖：
  - `WSB_MYSQL_DATA_DIR`
  - `WSB_REDIS_DATA_DIR`
  - `WSB_NACOS_INIT_DIR`
  - `WSB_NACOS_LOG_DIR`
  - `WSB_NACOS_DATA_DIR`
  - `WSB_PGVECTOR_DATA_DIR`
  - `WSB_PGVECTOR_INIT_SQL`

Docker 相关说明可查看 [docker/README.md](./docker/README.md)。

### 3. 初始化数据库

MySQL 业务表建议按以下顺序准备：

1. 执行基础结构脚本 [sql/schema/book_schema_alibaba_standard.sql](./sql/schema/book_schema_alibaba_standard.sql)
2. 再执行增量脚本目录 [sql/migrations](./sql/migrations)

RAG 向量库的初始化脚本为：

- [sql/rag/rag_pgvector_init.sql](./sql/rag/rag_pgvector_init.sql)

这份脚本会创建：

- `vector` 扩展
- `zhparser` 扩展
- `public.wsb_zhcfg` 中文分词配置
- `book_embeddings` 向量表与检索索引

注意：

- 该脚本会在 `pgvector` 容器首次初始化数据目录时自动执行。
- 如果 `E:/Docker/pgvector/data` 已经存在历史数据，初始化脚本不会自动重跑，此时需要你手动执行一次。

SQL 目录说明见 [sql/README.md](./sql/README.md)。

### 4. 准备 Nacos 配置

当前各服务的 `application.yml` 只保留了最小引导配置，真正运行时配置从 Nacos 加载。

服务会默认读取：

- `wsb-common.yml`
- `wsb-gateway-dev.yml`
- `wsb-user-dev.yml`
- `wsb-book-dev.yml`
- `wsb-community-dev.yml`
- `wsb-file-dev.yml`
- `wsb-rag-dev.yml`

仓库里已经提供可导入模板，见 [nacos/](./nacos)。

因此本地启动前，需要先在 Nacos 中准备这些 Data ID，对应内容通常包括：

- 服务端口
- MySQL 数据源
- Redis 连接
- RabbitMQ 连接
- 文件存储配置
- Sa-Token 配置
- Spring AI / 大模型 / Embedding 配置
- `rag.pgvector.*` 相关参数

当前本地基础环境中，Nacos 的配置持久化已经约定走 MySQL `nacos_config` 库；而服务注册相关的本地 raft / naming 数据仍会保存在 `E:/Docker/nacos/data`。

### 5. 启动后端服务

推荐在 IntelliJ IDEA 中直接运行以下启动类：

| 模块 | 启动类 |
| --- | --- |
| 网关 | `com.wsb.gateway.GatewayApplication` |
| 用户服务 | `com.wsb.user.UserApplication` |
| 图书服务 | `com.wsb.book.BookApplication` |
| 社区服务 | `com.wsb.community.WsbCommunityApplication` |
| 文件服务 | `com.wsb.file.WsbFileApplication` |
| RAG 服务 | `com.wsb.rag.WsbRagApplication` |

如果只想先验证编译，可以在仓库根目录执行：

```bash
mvn clean compile -DskipTests
```

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

- 前端开发服务器：`http://localhost:5173`
- 前端会把 `/api` 代理到：`http://localhost:8080`

前端独立说明见 [frontend/README.md](./frontend/README.md)。

## 接口与联调

项目已经接入 OpenAPI / Knife4j。网关启动后，通常可以通过以下地址查看接口文档：

```text
http://localhost:8080/doc.html
```

如果你在 Nacos 中把网关端口配置成了其他值，请按实际端口访问。

前端主要调用的接口域包括：

- `/v1/admin/*` 用户与认证
- `/v1/book/*` 图书、借阅、阅读记录
- `/v1/bookshelf/*` 书架
- `/v1/collect/*` 收藏
- `/v1/group/*` 群组、成员、社区借阅申请与审批
- `/v1/community/statistics/*` 统计分析
- `/v1/rag/*` AI 推荐与摘要能力
- `/v1/picture/*` 图片上传

## RAG 与 AI 能力

`wsb-rag` 是这个项目最有辨识度的模块。它不是单纯接一个大模型接口，而是把真实图书数据加工成“可检索、可推荐、可解释”的知识底座，再对外提供推荐与 AI 内容能力。

### 1. RAG 在这个项目里解决什么问题

传统图书管理系统通常只能按书名、作者、ISBN 做精确查询，但真实阅读需求往往是模糊的，例如：

- 我想找一本适合入门机器学习的书
- 我最近想看女性成长主题的小说
- 给我推荐几本和《活着》气质相近的作品

这类需求无法只靠关系型数据库字段匹配解决，所以本项目把 RAG 用在“书的理解与召回”上，而不是只做聊天问答：

- 先让模型为图书生成面向检索的摘要
- 再把书名、作者、关键词、分类、摘要拆成多个语义片段
- 把这些片段写入 `pgvector`
- 查询时同时走向量召回和中文关键词召回
- 最后再把候选结果重排后返回真实图书对象

也就是说，这里的 RAG 重点是“让书库可被自然语言检索”，而不是“做一个套壳聊天框”。

### 2. 当前已经落地的 RAG 能力

- 图书 AI 摘要生成：为单本书生成适合语义检索的中文摘要
- 网络书评聚合：搜索外部书评并整理成简短可读的评论摘要
- 相似图书推荐：根据某本书已有向量内容找相近作品
- 自然语言推荐：输入一句自然语言，返回符合语义的图书列表
- 私有书库推荐：支持只在当前用户自己的藏书范围内做推荐
- 向量化与混合检索：向量召回和关键词召回并行，再做融合排序

### 3. RAG 处理链路

项目里的 RAG 不是同步硬算，而是一条异步流水线：

1. 用户新增图书或触发 AI 处理
2. `wsb-book` / `wsb-rag` 把摘要任务投递到 RabbitMQ
3. `RagConsumer` 消费摘要任务，调用大模型生成摘要并写回图书表
4. 摘要完成后继续投递向量任务
5. `VectorServiceImpl` 将图书拆成多个片段并写入 `public.book_embeddings`
6. 用户发起推荐请求时，`RagServiceImpl` 先做查询扩写，再调用混合检索
7. 检索得到的 `bookId` 再回填成完整图书信息返回给前端

这样设计有两个好处：

- 图书录入和 AI 处理解耦，不会让用户保存图书时长时间阻塞
- 摘要、向量、推荐可以独立重试，失败时也更容易恢复

### 4. 为什么是 pgvector + zhparser + RabbitMQ

这套组合是为了兼顾“语义理解”和“中文关键词命中”：

- `pgvector`：负责语义相似度检索，解决“表达不同但语义接近”的召回问题
- `zhparser`：负责中文全文检索，解决书名、作者、主题词等关键词匹配问题
- `RabbitMQ`：负责把摘要生成和向量化改成异步任务，避免接口阻塞
- `Redis`：承担书评缓存、任务幂等锁等短期状态

所以它不是单一技术点，而是一套面向中文场景的检索架构。

### 5. 混合检索是怎么做的

当前推荐链路不是“只查向量库”，而是混合召回：

- 查询预处理：`QueryTextAnalyzer` 会抽取关键词并做有限扩写
- 向量召回：基于 `PgVectorStore` 做 similarity search
- 关键词召回：基于 `zhparser` 和 SQL 排序规则做中文全文匹配
- 结果融合：使用 RRF（Reciprocal Rank Fusion）合并多路候选
- 结果回填：通过 `RemoteBookService` 拿到完整图书 DTO，并按召回顺序返回

这种方案比“纯向量检索”更稳，原因是：

- 精确书名、作者、分类词不会被语义检索稀释
- 长尾中文表达依然能被全文检索兜住
- 语义相近但措辞不同的查询，又能被向量召回补足

### 6. 向量入库不是一整本书一条记录

为了提升召回质量，项目没有把一本书粗暴写成一条向量，而是按内容职责拆成多个 chunk：

- `identity`：书名、作者
- `subject`：关键词、中图分类、主题信息
- `summary`：AI 生成摘要

不同 chunk 还有不同权重，这样做的目的，是让“书名命中”“主题命中”“摘要命中”在检索时各自发挥作用，而不是互相稀释。

### 7. 为什么说它不只是 Demo

这个 RAG 模块已经和业务模块真正打通，而不是孤立样例：

- 推荐结果来自 `wsb-book` 的真实图书数据
- 可以限定在某个用户自己的藏书范围内
- 摘要生成后会回写图书表，参与后续检索
- 向量状态有 `PENDING / PROCESSING / COMPLETED` 生命周期
- 死信任务支持重新投递，幂等锁避免重复消费

这意味着它更接近一个“可交付的业务子系统”，而不只是演示 Spring AI 能跑起来。

### 8. 关键实现位置

- [wsb-modules/wsb-rag/src/main/java/com/wsb/rag/controller/RagController.java](./wsb-modules/wsb-rag/src/main/java/com/wsb/rag/controller/RagController.java)：推荐、相似图书、摘要、书评接口
- [wsb-modules/wsb-rag/src/main/java/com/wsb/rag/service/impl/RagServiceImpl.java](./wsb-modules/wsb-rag/src/main/java/com/wsb/rag/service/impl/RagServiceImpl.java)：查询扩写、多路召回、结果重排
- [wsb-modules/wsb-rag/src/main/java/com/wsb/rag/service/impl/VectorServiceImpl.java](./wsb-modules/wsb-rag/src/main/java/com/wsb/rag/service/impl/VectorServiceImpl.java)：向量写入、混合检索、相似图书召回
- [wsb-modules/wsb-rag/src/main/java/com/wsb/rag/consumer/RagConsumer.java](./wsb-modules/wsb-rag/src/main/java/com/wsb/rag/consumer/RagConsumer.java)：摘要与向量异步任务消费
- [wsb-modules/wsb-rag/src/main/java/com/wsb/rag/service/impl/BookAiContentServiceImpl.java](./wsb-modules/wsb-rag/src/main/java/com/wsb/rag/service/impl/BookAiContentServiceImpl.java)：摘要生成与网络书评聚合
- [wsb-modules/wsb-rag/src/main/java/com/wsb/rag/config/RagVectorConfig.java](./wsb-modules/wsb-rag/src/main/java/com/wsb/rag/config/RagVectorConfig.java)：`PgVectorStore` 配置
- [sql/rag/rag_pgvector_init.sql](./sql/rag/rag_pgvector_init.sql)：`vector`、`zhparser`、中文检索配置与向量表初始化脚本

## 常用验证命令

后端编译：

```bash
mvn -pl wsb-gateway,wsb-modules/wsb-user,wsb-modules/wsb-book,wsb-modules/wsb-community,wsb-modules/wsb-file,wsb-modules/wsb-rag -am -DskipTests compile
```

前端构建：

```bash
cd frontend
npm run build
```

Docker 配置检查：

```bash
docker compose -f docker/docker-compose.yml config
```

## 当前目录整理约定

- `docker/`：本地依赖环境与自定义镜像构建
- `nacos/`：Nacos 配置模板与导入说明
- `docs/`：仓库级说明文档
- `sql/`：建库、迁移与 RAG 初始化脚本
- `frontend/`：前端应用

## 注意事项

- 这是一个“代码在仓库、运行配置在 Nacos”的项目，直接 `git clone` 后通常还需要补齐本地配置中心内容。
- `docker/docker-compose.yml` 默认目录是我方当前 Win11 开发机路径约定，不同机器请按需覆写环境变量。
- `docker/mysql/init/nacos-mysql-schema.sql` 是 Nacos 官方 MySQL 表结构，空库首次启动时会自动导入。
- `pgvector` 的初始化脚本只会在全新数据目录上自动执行，老数据目录场景请手动补执行。
- 前端默认代理网关地址为 `http://localhost:8080`，如果网关端口有变更，记得同步调整 [frontend/vite.config.ts](./frontend/vite.config.ts)。

## 相关文档

- [docker/README.md](./docker/README.md)
- [nacos/README.md](./nacos/README.md)
- [docs/README.md](./docs/README.md)
- [docs/architecture/modules.md](./docs/architecture/modules.md)
- [sql/README.md](./sql/README.md)
- [frontend/README.md](./frontend/README.md)
