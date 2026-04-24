# Nacos Config Templates

这个目录存放项目本地开发使用的 Nacos 配置模板，文件名直接对应 Nacos 中的 `Data ID`。

当前建议导入的配置包括：

- `wsb-common.yml`
- `wsb-gateway-dev.yml`
- `wsb-user-dev.yml`
- `wsb-book-dev.yml`
- `wsb-community-dev.yml`
- `wsb-file-dev.yml`
- `wsb-rag-dev.yml`

使用方式：

1. 启动本地 Nacos
2. 登录控制台 `http://localhost:8848/nacos`
3. 在 `DEFAULT_GROUP` 下创建或导入同名 `Data ID`
4. 文件内容可直接复制粘贴，也可以按文件逐个导入

也可以直接执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\nacos\import-configs.ps1
```

说明：

- 这些文件基于当前项目实际使用的本地 Nacos 配置整理而来。
- 仓库里保留的是可提交版本，敏感值优先通过环境变量占位，不直接写死真实密钥。
- 如果你的本地端口、库名、账号或第三方服务不同，请按需修改。
- 当前基础设施约定中，Nacos 的配置数据持久化到 MySQL `nacos_config`，而 naming / raft 数据仍保留在本地挂载目录。

推荐配套环境变量：

- `WSB_MYSQL_JDBC_URL`
- `WSB_MYSQL_USERNAME`
- `WSB_MYSQL_PASSWORD`
- `WSB_REDIS_HOST`
- `WSB_REDIS_PORT`
- `WSB_REDIS_PASSWORD`
- `WSB_REDIS_DATABASE`
- `ALIYUN_ACCESS_KEY_ID`
- `ALIYUN_ACCESS_KEY_SECRET`
- `ALIYUN_ISBN_APPCODE`
- `GOOGLE_BOOKS_API_KEY`
- `TENCENT_COS_SECRET_ID`
- `TENCENT_COS_SECRET_KEY`
- `TENCENT_COS_BUCKET`
- `TENCENT_COS_REGION`
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `DEEPSEEK_CHAT_URL`
- `DEEPSEEK_API_KEY`
- `DASHSCOPE_EMBEDDING_BASE_URL`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_EMBEDDING_MODEL`
- `TAVILY_API_KEY`
- `RAG_PGVECTOR_JDBC_URL`
- `RAG_PGVECTOR_USERNAME`
- `RAG_PGVECTOR_PASSWORD`

如果你只是做本地联调，优先保证以下几项可用：

- MySQL
- Redis
- RabbitMQ
- PostgreSQL / pgvector
- DashScope Embedding Key
- DeepSeek API Key
