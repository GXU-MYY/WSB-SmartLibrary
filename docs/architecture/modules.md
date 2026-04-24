### 1. `wsb-user`（用户域）

核心职责：
- 用户注册、登录、鉴权
- 用户资料维护
- 用户基础信息对外查询

涉及表：
- `t_user`

### 2. `wsb-book`（图书域）

核心职责：
- 图书管理
- 书架管理
- 上下架管理
- 阅读记录
- 借阅记录
- 收藏能力

涉及表：
- `t_book`
- `t_shelf`
- `t_book_shelf`
- `t_book_label`
- `t_book_reading`
- `t_book_borrow`
- `t_collect`

补充说明：
- `t_collect` 已从原社交模块迁移到图书域。
- 收藏能力当前只保留图书收藏和书架收藏，不再支持评论收藏。

### 3. `wsb-community`（社区域）

核心职责：
- 群组管理
- 群成员管理
- 图书/书架分享
- 群内借阅申请与协作
- 统计聚合

涉及表：
- `t_group`
- `t_group_user`
- `t_share`
- `t_group_borrow_request`

### 4. `wsb-file`（文件域）

核心职责：
- 图片上传
- 文件资源管理

### 5. `wsb-rag`（智能检索域）

核心职责：
- 图书摘要生成
- 向量化
- 相似图书检索
- AI 推荐

补充说明：
- 向量数据当前落在 PostgreSQL + pgvector，不在主业务 MySQL 中。
