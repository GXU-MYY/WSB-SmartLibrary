### 1️⃣ wsb-user (用户域)
```
核心职责：用户管理、认证授权

表结构：
✅ t_user
```


### 2️⃣ wsb-book (图书域) - **核心域**
```
核心职责：图书管理、书架管理、阅读追踪

表结构：
✅ t_book
✅ t_shelf
✅ t_book_shelf
✅ t_book_label
✅ t_book_reading
```


### 3️⃣ wsb-borrow (借阅域)
```
核心职责：借阅管理

表结构：
✅ t_book_borrow

冗余字段：
- book_name (冗余，避免每次JOIN t_book)
```


### 4️⃣wsb-social（社交域）
```
核心职责：评论、点赞、收藏

表结构：
✅ t_comment - 评论表
✅ t_comment_like - 评论点赞表
✅ t_collect - 收藏表

冗余字段：
- book_name (t_comment中冗余)
- user_nickname (t_comment中冗余)
```


### 5️⃣ wsb-community (社区域)
```
核心职责：群组管理、内容分享

表结构：
✅ t_group
✅ t_group_user
✅ t_share
```