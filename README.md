## CityQuest 后端开发文档

本手册总结了当前 Spring Boot 后端的架构、依赖、配置、数据库、模块职责以及接口清单，并给出一步步实现指引，方便重新搭建与定制。

---

### 1. 系统概览
- **定位**：城市探索+社交+商城一体的互动平台，提供任务打卡、好友动态、即时聊天、商品兑换等能力。
- **技术栈**：Spring Boot 2.7 + MyBatis + MySQL 8 + Redis（在线用户/会话）+ WebSocket + 七牛云 OSS + 高德开放平台。
- **整体分层**：`controller`（REST API）→ `service`（业务逻辑）→ `mapper`（MyBatis）→ `entity`/`dto`（数据模型）→ `resources/mapper/*.xml`（SQL）。

---

### 2. 依赖与编译
关键依赖写在 `backend/pom.xml`：
- `spring-boot-starter-web / websocket / test`
- `mybatis-spring-boot-starter`
- `mysql-connector-java`
- `jjwt` + `jaxb` 组合（Java 17 兼容）
- `spring-boot-starter-data-redis`（在线用户、强制下线等）
- `commons-fileupload`、`apache-poi`（Excel 导入）
- `qiniu-java-sdk`

编译 & 运行：
```bash
mvn clean package
java -jar target/cityquest-backend-1.0.0.jar
```

---

### 3. 目录速览
```
backend/
├─ pom.xml
├─ src/main/java/com/cityquest
│  ├─ CityQuestApplication.java
│  ├─ config/         # CORS、拦截器、WebSocket
│  ├─ controller/     # REST 控制器
│  ├─ dto/            # 传输对象
│  ├─ entity/         # 实体和请求体
│  ├─ mapper/         # MyBatis 接口
│  ├─ service/impl    # 业务实现
│  ├─ util/           # Jwt、距离、雪花 ID
│  └─ websocket/      # 聊天 WebSocket
└─ src/main/resources
   ├─ application.yml
   ├─ mapper/*.xml
   ├─ init.sql        # 初始化库表
   └─ migration/*.sql # 额外迁移脚本
```

---

### 4. 环境与配置
1. **JDK 17**（与 `pom` 中 `<java.version>` 一致）
2. **Maven 3.8+**
3. **MySQL 8**：执行 `src/main/resources/init.sql`
4. **Redis**：默认使用 `localhost:6379`，若需自定义查看 `OnlineUserServiceImpl`
5. **七牛云**：
   - 创建空间 `cityquestshop`，绑定域名 `t5ny88t9i.hn-bkt.clouddn.com`
   - 在 `application.yml` 配置 `access-key` / `secret-key` / `bucket` / `domain`
   - 如域名无证书，将 `qiniu.use-https: false`
6. **高德地图**：
   - 申请 Web 服务 Key，写入 `amap.api-key`
7. **JWT 密钥**：`jwt.secret` 可自定义，注意同步前端验证逻辑（仅后端使用可自定）
8. **文件上传目录**：`file.upload.path` 默认 `d:/aiProject/CityQuest/upload/`，若迁移服务器需修改。
9. **服务器端口**：`server.port: 8080`，`context-path: /api`，所有接口实际路径均以 `/api` 开头。

---

### 5. 数据库模型概要
详见 `init.sql`，核心表：
- `user_info`：用户资料、积分、角色（user/admin）、状态。
- `task_info`：城市任务，含经纬度、奖励、封面、完成数。
- `record_info`：打卡记录，关联用户+任务、照片、审核状态。
- `friendship_info`：关注关系。
- `user_feed_info` / `feed_like_info` / `feed_comment_info`：动态、点赞、评论。
- `notification_info`：事件通知。
- `chat_session` / `chat_message`：私聊会话与消息。
- `product_category` / `product_info` / `exchange_order` / `exchange_order_item`：商城板块。

额外索引用于按用户、状态、类型快速查询。雪花 ID 生成在 `util/SnowflakeIdGenerator`.

---

### 6. 配置与基础设施
- **`config/WebConfig`**：注册 `JwtInterceptor`，映射 `/uploads/**` 到本地 `uploads` 目录。
- **`config/JwtInterceptor`**：拦截除登录/注册/静态资源外所有请求，验证 JWT，并结合 `OnlineUserService` 处理强制下线。
- **`config/CorsConfig`**：允许 `http://localhost:*` 前端跨域。
- **`config/WebSocketConfig`**：暴露 `/ws/chat`，由 `ChatWebSocketHandler` 处理消息。
- **`util/JwtUtil`**：签发/解析 token，默认 1 小时过期。
- **`service/QiniuService`**：封装上传、命名与校验逻辑，支持目录前缀（`avatars/`, `records/`, `tasks/`, `feeds/`）。

---

### 7. 模块职责与关键流程
#### 7.1 用户与鉴权
- `UserController`：登录/注册、获取与更新个人资料、上传头像、修改密码、排行榜。
- `UserService`：处理密码校验（MD5）、JWT 生成、积分更新。
- `OnlineUserService`：基于 Redis 维护在线状态、强制下线队列。

流程示例（登录）：
1. `POST /user/login` → 校验用户名/密码 → 生成 JWT → `OnlineUserService.markOnline`。
2. 前端后续请求在 `Authorization: Bearer <token>` 头中携带，`JwtInterceptor` 统一校验。

#### 7.2 任务与打卡
- `TaskController` / `TaskService`：任务 CRUD、热门任务、附近任务（借助 `DistanceUtil` 计算）。
- `RecordController` / `RecordService`：提交打卡（含位置、图片 OSS 上传）、按用户/任务分页、审核流程、完成校验。
- 积分奖励逻辑在 `RecordServiceImpl.submitRecord` & `TaskServiceImpl` 中完成（通过审核后叠加）。

#### 7.3 社交与动态
- `SocialController` / `SocialService`：
  - 关注/取消关注/列表
  - 动态：上传图片（OSS）、发布、列表（含图片数组解析）、点赞、评论、删除
  - 通知：分页查询、已读、未读数
- Mapper：`FeedMapper`, `FeedLikeMapper`, `FeedCommentMapper`, `FriendshipMapper`, `NotificationMapper`
- 删除动态时会级联删除点赞/评论（见 `SocialServiceImpl.deleteFeed`）。

#### 7.4 即时聊天
- `ChatController` + `chat.*` DTO
- `ChatService`：创建会话（Snowflake ID，确保用户对唯一）、获取消息、发送消息、已读更新、互相关注校验。
- WebSocket：`ChatWebSocketHandler` 推送实时消息，HTTP API 负责历史记录与初次会话创建。

#### 7.5 商城与积分兑换
- `MallController`（用户端）+ `AdminController`（后台管理）共享 `ProductService`/`ExchangeOrderService`。
- 功能：分类管理、商品 CRUD、上下架、积分兑换下单、订单审核与物流跟踪。

#### 7.6 地图与高德
- `MapController`：后端代理高德 POI 搜索，避免前端跨域和泄露 Key。
- `TaskService.getNearbyTasks` 结合经纬度过滤半径（km）。
- 前端地图定位直接使用高德 JS SDK，后端仅提供辅助数据。

#### 7.7 文件与七牛 OSS
- 所有图片统一走 `QiniuService.uploadFile(MultipartFile file, String directory, String fileName)`。
- 主要目录：
  - `avatars/`（用户头像）
  - `records/`（打卡照片）
  - `tasks/`（任务封面）
  - `feeds/`（动态图片）
- 返回值为完整 HTTP URL，前端用 `avatar.js` / `image.js` 统一转换。

---

### 8. API 速查（按控制器）
> 以下路径均需加上 `context-path` `/api`。

**UserController (`/user`)**
- `POST /login` 登录
- `POST /register` 注册
- `GET /info` 当前用户信息
- `GET /info/{id}` 指定用户
- `PUT /update` 更新用户
- `GET /rank` 积分榜
- `GET /list` 管理员获取列表
- `POST /logout` 退出
- `GET /profile` / `PUT /profile` 个人中心
- `POST /upload-avatar` 上传头像
- `POST /change-password` 修改密码

**TaskController (`/task`)**
- `GET /list` 条件分页
- `GET /info/{id}` 详情
- `POST /create` / `PUT /update` / `DELETE /delete/{id}`
- `GET /nearby` / `GET /hot`

**RecordController (`/record`)**
- `POST /submit` 提交打卡（含文件）
- `GET /user/list` / `GET /task/list`
- `GET /audit/list` 管理审核
- `POST /audit` 审核
- `GET /check` 用户是否完成任务

**SocialController (`/social`)**
- 关注：`POST /follow`、`/unfollow`、`GET /follow/check`
- 列表：`GET /following`、`/followers`
- 动态：
  - `POST /feed/upload-image`
  - `POST /feed`
  - `GET /feed`
  - `POST /feed/{id}/like`、`/unlike`
  - `POST /feed/{id}/comment`
  - `GET /feed/{id}/comments`
  - `DELETE /feed/{id}`
- 通知：`GET /notifications`、`POST /notifications/{id}/read`、`POST /notifications/read-all`、`GET /notifications/unread-count`

**ChatController (`/chat`)**
- `GET /sessions`
- `POST /sessions` 创建
- `GET /messages?sessionId=`
- `POST /messages`
- `POST /sessions/{sessionId}/read`
- `GET /mutual-follow`

**MallController (`/mall`)**
- `GET /categories`
- `GET /products`、`GET /products/{id}`
- `POST /orders`
- `GET /orders`
- `GET /orders/{id}`
- `POST /orders/{id}/cancel`

**AdminController (`/admin`)**（管理后台入口）
- 数据看板：`GET /statistics`、`/statistics/task-types`、`/activities/recent`
- 任务：`GET /tasks`、`POST /tasks`、`PUT /tasks/update`、`DELETE /tasks/{id}`、批量状态/删除/导入/导出模板、`POST /tasks/upload-image`
- 用户：`GET /users`、`PUT /users/{id}`、`DELETE /users/{id}`、状态更新/批量操作/重置密码、强制下线、在线列表
- 记录审核：`GET /records`、`POST /records/{id}/approve|reject`、批量通过/拒绝/删除
- 活动日志：`GET /activity-logs`、`POST /activity-logs/export`
- 商城管理：分类、商品、订单 CRUD 与状态更新
- 退出：`POST /logout`

**MapController (`/admin/map`)**
- `GET /search` 代理高德地点检索

---

### 9. 实现路径（推荐步骤）

| 阶段 | 功能 | 关键类/文件 | 任务要点 |
| --- | --- | --- | --- |
| 0 | **环境 & 工程骨架** | `pom.xml`, `CityQuestApplication` | 配置 JDK17、Maven、本地 MySQL & Redis，`mvn archetype` 或直接复制现有工程骨架，确保能 `mvn spring-boot:run` 空跑。 |
| 1 | **配置层与基础设施** | `application.yml`, `CorsConfig`, `WebConfig`, `JwtInterceptor`, `WebSocketConfig`, `logging` 设置 | 先把端口、数据源、JWT、OSS、CORS、静态资源映射写好；`WebConfig` 注册拦截器；`JwtInterceptor` 只放行登录/注册/静态资源；WebSocket 暂可空 handler 保证 bean 正常注入。 |
| 2 | **通用工具与组件** | `util/JwtUtil`, `util/SnowflakeIdGenerator`, `util/DistanceUtil`, `service/QiniuService`+`impl` | 先实现 JWT 签发/解析、雪花 ID、经纬度计算；`QiniuService` 抽象上传方法（含目录、大小、类型校验）。这些会被后续模块频繁依赖。 |
| 3 | **数据库落地 & MyBatis** | `resources/init.sql`, `entity/*`, `mapper/*.java`, `mapper/*.xml` | 执行 `init.sql`; 逐张表创建实体、Mapper 接口、XML。推荐顺序：`user_info` → `task_info` → `record_info` → 社交表 → 聊天表 → 商城表。每完成一张表即可写最简单的 CRUD SQL 以便后续 service 直接调用。 |
| 4 | **用户认证模块** | `UserService`, `UserServiceImpl`, `OnlineUserService`, `UserController`, `entity/dto/*` | 先做登录/注册→JWT→Redis 在线标记，再做 `/user/info`、`/user/profile`、`/upload-avatar`（依赖 `QiniuService`）。此阶段完成后，所有需要鉴权的接口都能复用 `JwtInterceptor`。 |
| 5 | **任务模块** | `TaskService`, `TaskServiceImpl`, `TaskMapper`, `TaskController`, `AdminController` 的任务段 | 实现任务 CRUD、分页、热门、附近逻辑（`DistanceUtil`）；后台批量操作和导入可放在此阶段末尾。 |
| 6 | **打卡记录模块** | `RecordService`, `RecordServiceImpl`, `RecordController`, `AdminController` 审核段 | 打卡提交（含 OSS 上传）、按用户/任务查询、审核流转、积分累加、统计字段更新（如任务完成人数）。先保证 `/record/submit` 正常，再拓展后台审核接口。 |
| 7 | **社交与动态** | `SocialService`, `SocialServiceImpl`, `SocialController`, `FriendshipMapper`, `FeedMapper`, `NotificationMapper` | 按顺序实现：关注/取关→动态上传/发布/列表→点赞/评论→通知系统。删除动态时记得联合删除点赞评论（`FeedLikeMapper.deleteByFeedId` 等）。 |
| 8 | **即时聊天** | `ChatService`, `ChatServiceImpl`, `ChatController`, `ChatMessageMapper`, `ChatSessionMapper`, `websocket/*` | 先实现会话/消息的 MyBatis 层，再打通 HTTP API，最后接入 WebSocket 即时推送。注意 `sessionId` 统一用 `String` 接参数、内部转 Long。 |
| 9 | **商城模块** | `ProductCategoryService`, `ProductService`, `ExchangeOrderService` 及 `MallController`/`AdminController` 对应段 | 先搭建分类与商品 CRUD，再做兑换订单（库存 & 积分扣减逻辑在 `ExchangeOrderServiceImpl`）。 |
| 10 | **地图 & 高德代理** | `MapController`, `TaskService.getNearbyTasks`（调用距离过滤） | 实现 `/admin/map/search` 代理高德接口，前端读取 Key；后端确保地点/经纬度数据在任务 & 记录里充足。 |
| 11 | **后台综合接口** | `AdminController` 统计、活动日志、批量操作段，`RecordService` 的导出等 | 基于前面模块输出的数据，补齐后台统计/导出/批量接口。 |
| 12 | **测试 & 上线** | Postman/Swagger/curl、集成测试类（可选） | 逐模块回归，重点覆盖：登录鉴权、OSS 上传、定位接口、WebSocket 连接、批量操作、商城兑换。记录常见异常并验证日志配置。 |

> Tips：若要更细化，可在每阶段内部按 “Controller → Service → Mapper → 单测/联调” 顺序推进，确保功能闭环再进入下一阶段。

---

### 9.1 接口实现顺序与对应类（强推荐按此顺序逐个打通）

以下按“从易到难、从核心到扩展”的顺序罗列接口。每个接口列出需要修改/依赖的类，按 Controller → Service → Mapper/XML → Entity/DTO 的顺序给出。

1) 用户认证与资料（优先完成，所有鉴权接口依赖）
- POST `/user/register`
  - Controller: `UserController.register`
  - Service: `UserService.register`, Impl: `UserServiceImpl`
  - Mapper: `UserMapper` + `mapper/UserMapper.xml`（insert 用户）
  - Entity/DTO: `UserInfo`, `RegisterRequest`
- POST `/user/login`
  - Controller: `UserController.login`
  - Service: `UserService.login`（签发 JWT，标记在线）; `OnlineUserService.markOnline`
  - Mapper: `UserMapper`（select by username/密码）
  - Util: `JwtUtil`, `SnowflakeIdGenerator`（如需要生成会话/日志 ID）
  - Entity/DTO: `LoginRequest`, `UserInfo`
- GET `/user/info`
  - Controller: `UserController.getCurrentUserInfo`
  - Service: `UserService.getCurrentUserInfo`
  - Util: `JwtUtil`（从 token 解析 userId）
  - Mapper: `UserMapper`（select by id）
- GET `/user/profile`
  - Controller: `UserController.getProfile`
  - Service: `UserService.getUserById`, `RecordService.getUserRecords`（最近记录），`RecordMapper.selectCount`
  - Mapper: `UserMapper`, `RecordMapper`
  - Entity: `UserInfo`, `RecordInfo`, `TaskInfo`
- PUT `/user/profile`
  - Controller: `UserController.updateProfile`
  - Service: `UserService.updateUser`
  - Mapper: `UserMapper`（update）
  - DTO: `UserProfileUpdateRequest`
- POST `/user/upload-avatar`
  - Controller: `UserController.uploadAvatar`
  - Service: `QiniuService.uploadFile`, `UserService.updateUser`
  - Mapper: `UserMapper`（update avatar）
  - Entity: `UserInfo`
- POST `/user/change-password`
  - Controller: `UserController.changePassword`
  - Service: `UserService.changePassword`
  - Mapper: `UserMapper`（update password）
- POST `/user/logout`
  - Controller: `UserController.logout`
  - Service: `OnlineUserService.markOffline`, `UserService.updateUser(status=0)`

2) 任务模块（任务是业务核心，推荐次优先）
- POST `/task/create`
  - Controller: `TaskController.createTask`（或后台 `AdminController.createTask`）
  - Service: `TaskService.createTask`
  - Mapper/XML: `TaskMapper` + `mapper/TaskMapper.xml`（insert）
  - Entity: `TaskInfo`
- PUT `/task/update`
  - Controller: `TaskController.updateTask`（或后台 `AdminController.updateTask`）
  - Service: `TaskService.updateTask`
  - Mapper: `TaskMapper`（update）
- DELETE `/task/delete/{id}`
  - Controller: `TaskController.deleteTask`（或后台 `AdminController.deleteTask`）
  - Service: `TaskService.deleteTask`
  - Mapper: `TaskMapper`（delete）
- GET `/task/info/{id}`
  - Controller: `TaskController.getTaskInfo`
  - Service: `TaskService.getTaskById`
  - Mapper: `TaskMapper`（select by id）
- GET `/task/list?type=&status=&page=&pageSize=&keyword=&userId=`
  - Controller: `TaskController.getTaskList`
  - Service: `TaskService.getTaskList`
  - Mapper: `TaskMapper`（分页、条件查询）
- GET `/task/hot`
  - Controller: `TaskController.getHotTasks`
  - Service: `TaskService.getHotTasks`
  - Mapper: `TaskMapper`
- GET `/task/nearby?longitude=&latitude=&radius=`
  - Controller: `TaskController.getNearbyTasks`
  - Service: `TaskService.getNearbyTasks`
  - Util: `DistanceUtil`
  - Mapper: `TaskMapper`
- POST `/admin/tasks/upload-image`（后台任务封面上传）
  - Controller: `AdminController.uploadTaskImage`
  - Service: `QiniuService.uploadFile`

3) 打卡记录（与任务强相关，做完任务后立即做打卡）
- POST `/record/submit`
  - Controller: `RecordController.submitRecord`
  - Service: `RecordService.submitRecord`（含积分逻辑）、`QiniuService.uploadFile`（可选）
  - Mapper: `RecordMapper`（insert）
  - Entity: `RecordInfo`
- GET `/record/user/list?userId=&page=&pageSize=`
  - Controller: `RecordController.getUserRecords`
  - Service: `RecordService.getUserRecords`
  - Mapper: `RecordMapper`
- GET `/record/task/list?taskId=&page=&pageSize=`
  - Controller: `RecordController.getTaskRecords`
  - Service: `RecordService.getTaskRecords`
  - Mapper: `RecordMapper`
- GET `/record/audit/list?status=&page=&pageSize=`（后台审核列表）
  - Controller: `RecordController.getAuditList` 或 `AdminController.getRecordList`
  - Service: `RecordService.getAuditList`
  - Mapper: `RecordMapper`
- POST `/record/audit`（或后台批量：见 `AdminController` 批量接口）
  - Controller: `RecordController.auditRecord`，后台见 `AdminController` 各审核接口
  - Service: `RecordService.auditRecord`
  - Mapper: `RecordMapper`（update 审核状态）
- GET `/record/check?userId=&taskId=`
  - Controller: `RecordController.checkTaskCompletion`
  - Service: `RecordService.checkTaskCompletion`
  - Mapper: `RecordMapper`

4) 社交-关注/粉丝（动态之前先完成关注）
- POST `/social/follow` / `/social/unfollow`
  - Controller: `SocialController.followUser` / `unfollowUser`
  - Service: `SocialService.followUser` / `unfollowUser`
  - Mapper: `FriendshipMapper`（insert/delete），`NotificationMapper`（可选）
  - Entity: `FriendshipInfo`, `NotificationInfo`
- GET `/social/follow/check?followeeId=`
  - Controller: `SocialController.checkFollow`
  - Service: `SocialService.isFollowing`
  - Mapper: `FriendshipMapper`
- GET `/social/following` / `/social/followers`
  - Controller: `SocialController.getFollowingList` / `getFollowerList`
  - Service: `SocialService.getFollowingList` / `getFollowerList`
  - Mapper: `FriendshipMapper`

5) 社交-动态（图片上传 → 发布 → 列表 → 点赞/评论 → 删除）
- POST `/social/feed/upload-image`
  - Controller: `SocialController.uploadFeedImage`
  - Service: `QiniuService.uploadFile`
- POST `/social/feed`
  - Controller: `SocialController.publishFeed`
  - Service: `SocialService.publishFeed`
  - Mapper: `FeedMapper`（insert）
  - Entity: `UserFeedInfo`
- GET `/social/feed?page=&pageSize=&type=public|following`
  - Controller: `SocialController.getFeedList`
  - Service: `SocialService.getFeedList`（解析 imageUrl → imageList）
  - Mapper: `FeedMapper`
- POST `/social/feed/{feedId}/like` / `/unlike`
  - Controller: `SocialController.likeFeed` / `unlikeFeed`
  - Service: `SocialService.likeFeed` / `unlikeFeed`
  - Mapper: `FeedLikeMapper`（insert/delete）
  - Entity: `FeedLikeInfo`
- POST `/social/feed/{feedId}/comment`
  - Controller: `SocialController.commentFeed`
  - Service: `SocialService.commentFeed`
  - Mapper: `FeedCommentMapper`（insert）
  - Entity: `FeedCommentInfo`
- GET `/social/feed/{feedId}/comments`
  - Controller: `SocialController.getCommentList`
  - Service: `SocialService.getCommentList`
  - Mapper: `FeedCommentMapper`
- DELETE `/social/feed/{feedId}`
  - Controller: `SocialController.deleteFeed`
  - Service: `SocialService.deleteFeed`（同时清理点赞/评论）
  - Mapper: `FeedMapper`, `FeedLikeMapper.deleteByFeedId`, `FeedCommentMapper.deleteByFeedId`

6) 通知（与社交互动、审核联动，可在动态完成后实现）
- GET `/social/notifications`
  - Controller: `SocialController.getNotificationList`
  - Service: `SocialService.getNotificationList`
  - Mapper: `NotificationMapper`
- POST `/social/notifications/{id}/read`
  - Controller: `SocialController.markNotificationAsRead`
  - Service: `SocialService.markNotificationAsRead`
  - Mapper: `NotificationMapper`（update read_status）
- POST `/social/notifications/read-all`
  - Controller: `SocialController.markAllNotificationsAsRead`
  - Service: `SocialService.markAllNotificationsAsRead`
  - Mapper: `NotificationMapper`
- GET `/social/notifications/unread-count`
  - Controller: `SocialController.getUnreadNotificationCount`
  - Service: `SocialService.getUnreadNotificationCount`
  - Mapper: `NotificationMapper`

7) 聊天（会话/消息 → 历史 → 已读 → WebSocket）
- GET `/chat/sessions` / POST `/chat/sessions`
  - Controller: `ChatController.listSessions` / `createSession`
  - Service: `ChatService.listUserSessions` / `getOrCreateSession`
  - Mapper: `ChatSessionMapper`
  - Entity/DTO: `ChatSession`, `ChatSessionDTO`, `ChatSessionCreateRequest`
- GET `/chat/messages?sessionId=&page=&pageSize=` / POST `/chat/messages`
  - Controller: `ChatController.listMessages` / `sendMessage`
  - Service: `ChatService.getSessionMessages` / `sendMessage`
  - Mapper: `ChatMessageMapper`
  - Entity/DTO: `ChatMessage`, `ChatMessageDTO`, `ChatSendRequest`
  - 注意：`sessionId` 用 String 接收 → 内部 `Long.parseLong`
- POST `/chat/sessions/{sessionId}/read`
  - Controller: `ChatController.markAsRead`
  - Service: `ChatService.markSessionMessagesAsRead`
  - Mapper: `ChatMessageMapper`
- WebSocket `/ws/chat`
  - Config: `WebSocketConfig`
  - Handler: `ChatWebSocketHandler`，Interceptor: `ChatHandshakeInterceptor`
  - Service: `ChatService` 推送联动

8) 商城（分类/商品/订单，先用户侧，再后台侧）
- 用户侧 `/mall/categories` `/mall/products` `/mall/products/{id}`
  - Controller: `MallController.getCategories/getProducts/getProductDetail`
  - Service: `ProductCategoryService.getAllCategories`, `ProductService.getProductList/getProductById`
  - Mapper: `ProductCategoryMapper`, `ProductMapper`
  - Entity: `ProductCategory`, `ProductInfo`
- 用户侧订单 `/mall/orders`（POST/GET） `/mall/orders/{id}` `/mall/orders/{id}/cancel`
  - Controller: `MallController.createOrder/getMyOrders/getOrderDetail/cancelOrder`
  - Service: `ExchangeOrderService.createOrder/getOrderList/getOrderById/cancelOrder`
  - Mapper: `ExchangeOrderMapper`, `ExchangeOrderItemMapper`
  - Entity: `ExchangeOrder`, `ExchangeOrderItem`
- 后台分类/商品/订单（`/admin/mall/*`）
  - Controller: `AdminController`（对应 mall 段接口）
  - Service/Mapper 同用户侧服务

9) 后台综合能力（统计 / 审核 / 批量 / 导出）
- 统计与活动：`GET /admin/statistics` `/admin/statistics/task-types` `/admin/activities/recent`
  - Controller: `AdminController.getStatistics/getTaskTypeDistribution/getRecentActivities`
  - Service: 聚合 `UserService/TaskService/RecordService` 等
  - Mapper: `UserMapper`, `TaskMapper`, `RecordMapper`
- 记录审核与批量：`/admin/records` 列表、`/admin/records/{id}/approve|reject`、`/admin/records/batch/*`
  - Controller: `AdminController`（多方法）
  - Service: `RecordService.auditRecord/batch.../delete...`
  - Mapper: `RecordMapper`
- 活动日志导出：`POST /admin/activity-logs/export`
  - Controller: `AdminController.exportActivityLogs`
  - Service: 复用 `RecordService`

10) 地图与高德代理
- GET `/admin/map/search?keywords=&city=&offset=&page=`
  - Controller: `MapController.search`
  - Config: `application.yml` 的 `amap.api-key`
  - HTTP 客户端：`RestTemplate`

实现与联调建议：
- 每完成一个接口：用 Postman 校验 → 前端联调 → 回归 `JwtInterceptor`（确保未误拦截静态资源，如 `/uploads/**`）。
- 涉及 OSS 上传的接口：先测试七牛直传返回 URL，再写入对应业务表字段。
- 涉及分页的接口：统一返回 `{items/list, total}` 字段，便于前端复用。

---

### 10. 常见问题速记
- **OSS 域名证书报错**：`qiniu.use-https` 设为 `false`，前端提供 HTTP 访问；或在七牛配置自签证书。
- **静态资源 401**：确保 `JwtInterceptor` 排除了 `/uploads/**`，并在前端错误处理中忽略图片 401。
- **高德定位失败**：拉长 `AMap.Geolocation` 超时时间并实现降级策略（已在前端处理）；后端只需保证 `api-key` 有效。
- **sessionId 类型**：聊天接口使用 `String` 接收，内部 `Long.parseLong`，避免前端传字符串导致 400。
- **雪花 ID 冲突**：`SnowflakeIdGenerator` 使用固定 `workerId/datacenterId`，部署多实例需配置不同编号。

---

如需进一步扩展（如多租户、权限细粒度、内容审核、ES 搜索），建议按模块拆分微服务时复用以上分层思路。祝开发顺利！

