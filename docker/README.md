# Docker Files

`docker/` 目录现在只保留两类内容：

- `docker/docker-compose.yml`
  Win11 本地开发统一依赖环境，包含 `mysql`、`redis`、`nacos`、`rabbitmq`、`pgvector`
- `docker/Dockerfile.pgvector`
  `pgvector + zhparser` 自定义镜像构建文件

常用命令：

```bash
docker compose -f docker/docker-compose.yml up -d --build
docker compose -f docker/docker-compose.yml up -d mysql redis nacos rabbitmq
docker compose -f docker/docker-compose.yml up -d --build pgvector
```

默认目录约定：

- `mysql -> E:/Docker/mysql`
- `redis -> E:/Docker/redis`
- `nacos -> E:/Docker/nacos`
- `pgvector -> E:/pgvector`

可通过环境变量覆盖：

- `WSB_MYSQL_DATA_DIR`
- `WSB_MYSQL_INIT_DIR`
- `WSB_REDIS_DATA_DIR`
- `WSB_NACOS_INIT_DIR`
- `WSB_NACOS_LOG_DIR`
- `WSB_NACOS_DATA_DIR`
- `WSB_PGVECTOR_DATA_DIR`
- `WSB_PGVECTOR_INIT_SQL`

补充说明：

- `pgvector` 初始化脚本默认使用 `sql/rag/rag_pgvector_init.sql`
- Compose 项目名固定为 `wsb`
- `rabbitmq` 现在使用 named volume `wsb_rabbitmq_data`
- 不再保留单独的 `docker/pgvector/docker-compose.yml`
