# 学生毕业设计管理系统后端

学生毕业设计管理系统的 Spring Boot 后端，为学院管理员、指导教师和应届毕业生提供毕业设计全流程线上管理能力。项目采用 RESTful API 与 Vue 前端交互，使用 MySQL 持久化业务数据，并集成腾讯云 COS 存储头像和毕业设计相关文档。

## 项目状态

项目目前处于持续开发阶段，已实现用户认证、用户管理、文件上传、课题管理、选题管理、任务书管理、过程文档管理和答辩管理。成绩、统计导出等业务接口仍在后续开发中。

## 已实现功能

### 用户与认证

- 用户注册、登录、退出和查询当前登录用户
- 公开注册支持学生和教师类型，管理员账号只能由管理员新增
- 基于 Sa-Token 的登录态管理
- 用户密码使用 BCrypt 哈希存储，历史明文密码会在成功登录后自动升级
- 管理员接口使用 Sa-Token 角色校验，普通用户不能自行提升角色
- 使用 `permission.json` 静态维护学生、教师和管理员的角色权限码
- 管理员新增、删除、编辑、查询和分页查询用户
- 普通用户查询和编辑本人信息
- 用户列表与用户详情脱敏，密码字段不返回给前端

### 文件管理

- 上传头像和毕业设计相关附件
- 将文件上传至腾讯云 COS
- 文件记录持久化到 `file_record` 表
- 图片存入 `avatar` 目录，文档存入 `attachment` 目录
- 支持替换已上传文件和单独修改文件名
- 限制文件大小、扩展名、文件类型和文件所有者
- COS 上传或数据库保存失败时执行补偿处理
- 返回文件 ID、访问地址、COS 对象键、文件名、大小和媒体类型

当前支持的上传格式：

- 图片：`jpg`、`jpeg`、`png`、`gif`、`webp`
- 文档：`pdf`、`doc`、`docx`
- 单文件最大：`50 MB`

### 课题管理

- 教师创建、编辑和删除课题草稿
- 教师提交课题审核
- 管理员审核通过或驳回课题
- 学生只查看审核通过的课题
- 支持按名称、类型、指导教师和状态筛选
- 提供学生可用的教师筛选列表
- 返回计划人数、已选人数和剩余名额
- 教师只能维护自己发布的课题
- 记录审核人、审核意见和审核时间

### 选题管理

- 学生向审核通过的课题提交选题申请
- 学生可以取消待审核选题，并在取消或驳回后重新申请
- 一名学生同时只能保留一个待审核或已通过的选题
- 教师审核本人课题下的选题申请
- 课题通过人数不能超过可选人数
- 学生只能查看本人选题，教师只能查看本人课题的选题
- 管理员可以查看全部选题记录

### 任务书管理

- 教师为本人课题下已审核通过的选题创建任务书草稿
- 一份选题只能创建一份任务书
- 教师可以编辑、删除和下发本人任务书草稿
- 任务书下发后记录下发时间，并且不能再编辑或删除
- 任务书支持正文、截止时间和文档附件
- 附件必须来自当前教师上传的 `attachment` 类型文件
- 学生只能查看本人选题对应的已下发任务书
- 教师可以查看本人全部任务书，管理员可以查看全部任务书
- 返回学生、教师、课题和附件等关联展示信息

### 过程文档管理

- 学生为本人已通过的选题提交开题报告、中期检查、论文初稿和论文终稿
- 同一文档类型支持多版本提交并保留历史记录
- 待审核文档不能重复提交，驳回后可以提交新版本
- 文档审核通过后该类型文档锁定，不能继续提交
- 教师只能审核本人课题学生的过程文档
- 学生只能查看本人文档，教师只能查看本人课题学生的文档
- 管理员可以查看全部过程文档
- 返回文档版本、审核状态、审核意见、学生、教师、课题和文件信息

### 答辩管理

- 管理员创建、编辑和删除答辩组
- 配置答辩组名称、答辩时间、地点和备注
- 一个答辩组可以包含多名答辩教师
- 一个答辩组可以包含多名学生及其选题
- 一名教师可以参加多个答辩组
- 一名学生只能分配到一个答辩组
- 分配学生时自动关联其审核通过的选题
- 教师查看本人参加的答辩安排
- 学生查看本人的答辩时间和地点
- 管理员查看全部答辩组

## 技术栈

| 技术 | 版本或说明 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.5.16 |
| Spring MVC | RESTful API |
| MyBatis-Plus | 3.5.15 |
| MySQL | 8.0 |
| Sa-Token | 1.45.0 |
| 腾讯云 COS | cos_api 5.6.227 |
| 接口文档 | Springdoc OpenAPI、Knife4j |
| 测试 | Spring Boot Test、H2 |
| 构建工具 | Maven Wrapper |

项目基于 Spring Boot 3，部署时应使用 Tomcat 10.1 或 Spring Boot 内嵌的兼容版本，不能使用 Tomcat 9。

## 系统架构

```text
Vue 前端
    |
    | HTTP / JSON / multipart
    v
Spring MVC Controller
    |
    v
Service
    |--------------------|
    v                    v
MyBatis-Plus         腾讯云 COS
    |
    v
MySQL
```

接口统一返回以下结构：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

业务异常通过全局异常处理器转换为友好提示，不向前端返回数据库信息或内部异常堆栈。

## 项目结构

```text
xsbysj-backend
├─ sql/
│  └─ create_table.sql            数据库结构的唯一权威定义
├─ src/main/java/org/lc/xsbysjbackend/
│  ├─ common/                    统一响应、分页参数、全局异常处理
│  ├─ config/                    MyBatis-Plus、Sa-Token、CORS、JSON、COS 配置
│  ├─ controller/                REST 接口
│  ├─ exception/                 业务异常与错误码
│  ├─ manager/                   COS 等外部服务封装
│  ├─ mapper/                    MyBatis-Plus Mapper
│  ├─ model/
│  │  ├─ dto/                    请求对象
│  │  ├─ entity/                 数据库实体
│  │  ├─ enums/                  枚举
│  │  └─ vo/                     响应对象
│  └─ service/                   业务服务及实现
├─ src/main/resources/
│  ├─ mapper/                    MyBatis XML
│  └─ application.yaml           应用主配置
└─ src/test/                     单元测试与接口测试
```

## 数据库

数据库名称为 `xsbysj`。建表脚本位于：

```text
sql/create_table.sql
```

当前数据表：

| 表名 | 用途 | 状态 |
| --- | --- | --- |
| `user` | 用户账号、角色和头像 | 已实现 |
| `file_record` | COS 文件上传记录 | 已实现 |
| `topic` | 毕业设计课题 | 已实现 |
| `selection` | 学生选题及审核 | 已实现 |
| `assignment` | 任务书 | 已实现 |
| `report` | 过程文档、版本及审核状态 | 已实现 |
| `defense_group` | 答辩组、时间与地点 | 已实现 |
| `defense_group_teacher` | 答辩组教师关联 | 已实现 |
| `defense_group_student` | 答辩组学生及选题关联 | 已实现 |
| `score` | 毕业设计成绩 | 表结构已预留 |

数据库结构约定：

- 所有业务表使用 MyBatis-Plus 雪花 ID。
- 表之间不设置数据库外键。
- 每张表均包含 `createTime`、`EditTime`、`UpdateTime`。
- `createTime` 和 `EditTime` 默认使用 `CURRENT_TIMESTAMP`。
- `UpdateTime` 默认使用 `CURRENT_TIMESTAMP`，并配置 `ON UPDATE CURRENT_TIMESTAMP`。

## 快速开始

### 1. 环境要求

- JDK 17
- MySQL 8.0
- 腾讯云 COS 存储桶
- Git

Maven 使用项目自带的 Maven Wrapper，无需单独安装。

### 2. 克隆项目

```bash
git clone https://github.com/Chosen1Lc20/xsbysj-backend.git
cd xsbysj-backend
```

### 3. 初始化数据库

使用 MySQL 客户端执行：

```sql
SOURCE sql/create_table.sql;
```

也可以直接在 MySQL Workbench 或 DataGrip 中运行 `sql/create_table.sql`。

### 4. 配置本地环境

数据库连接地址支持以下环境变量：

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
```

本地数据库密码和腾讯云 COS 凭据建议放入不会被 Git 跟踪的 `application-local.yaml`。配置示例：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:your-password}

cos:
  client:
    host: https://your-bucket.cos.ap-region.myqcloud.com
    secret-id: ${COS_SECRET_ID:your-secret-id}
    secret-key: ${COS_SECRET_KEY:your-secret-key}
    region: ap-region
    bucket: your-bucket
```

请勿将真实的数据库密码、COS SecretId 或 SecretKey 提交到 Git。

### 5. 启动项目

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux：

```bash
./mvnw spring-boot:run
```

默认服务地址：

```text
http://localhost:8080
```

### 6. 接口文档

项目启动后可访问：

- Knife4j：`http://localhost:8080/doc.html`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 主要接口

### 用户接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/user/register` | 用户注册 |
| `POST` | `/api/user/login` | 用户登录 |
| `POST` | `/api/user/logout` | 退出登录 |
| `GET` | `/api/user/me` | 查询当前用户 |
| `POST` | `/api/user/add` | 管理员新增用户 |
| `POST` | `/api/user/delete` | 管理员删除用户 |
| `GET` | `/api/user/get` | 管理员查询用户 |
| `GET` | `/api/user/get/vo` | 查询脱敏用户信息 |
| `POST` | `/api/user/update` | 编辑用户信息 |
| `POST` | `/api/user/list/page/vo` | 分页查询用户 |

### 文件接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/file/upload` | 上传头像或附件 |
| `POST` | `/api/file/update` | 替换文件或修改文件名 |

上传接口使用 `multipart/form-data`，文件字段名为 `file`。

编辑文件接口参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `id` | 是 | 文件主键 |
| `file` | 否 | 新文件；传入后替换原文件 |
| `fileName` | 否 | 新文件名；不传新文件时使用 |

`file` 和 `fileName` 至少传入一个。用户只能编辑自己上传的文件。

### 课题接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/topic/add` | 教师新增课题 |
| `POST` | `/api/topic/update` | 教师编辑本人课题 |
| `POST` | `/api/topic/delete` | 教师删除本人课题 |
| `POST` | `/api/topic/submit` | 教师提交课题审核 |
| `POST` | `/api/topic/review` | 管理员审核课题 |
| `GET` | `/api/topic/get/vo` | 查询课题详情 |
| `POST` | `/api/topic/list/page/vo` | 按当前角色分页查询课题 |

### 选题接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/selection/apply` | 学生申请选题 |
| `POST` | `/api/selection/cancel` | 学生取消待审核选题 |
| `POST` | `/api/selection/review` | 教师审核选题 |
| `GET` | `/api/selection/get/vo` | 查询选题详情 |
| `POST` | `/api/selection/list/page/vo` | 按当前角色分页查询选题 |

### 任务书接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/assignment/add` | 教师为已通过的选题创建任务书 |
| `POST` | `/api/assignment/update` | 教师编辑本人任务书草稿 |
| `POST` | `/api/assignment/delete` | 教师删除本人任务书草稿 |
| `POST` | `/api/assignment/issue` | 教师下发任务书 |
| `GET` | `/api/assignment/get/vo` | 查询任务书详情 |
| `POST` | `/api/assignment/list/page/vo` | 按当前角色分页查询任务书 |

### 过程文档接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/document/submit` | 学生提交或重新提交过程文档 |
| `POST` | `/api/document/review` | 教师审核本人课题学生的文档 |
| `GET` | `/api/document/get/vo` | 查询过程文档详情 |
| `POST` | `/api/document/list/page/vo` | 按当前角色分页查询文档及历史版本 |

### 答辩接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| `POST` | `/api/defense/group/add` | 管理员创建答辩组并分配教师和学生 |
| `POST` | `/api/defense/group/update` | 管理员编辑答辩组及成员 |
| `POST` | `/api/defense/group/delete` | 管理员删除答辩组 |
| `GET` | `/api/defense/group/get/vo` | 查询答辩组详情 |
| `POST` | `/api/defense/group/list/page/vo` | 按当前角色分页查询答辩安排 |

## 测试与构建

运行全部测试：

```powershell
.\mvnw.cmd test
```

打包项目：

```powershell
.\mvnw.cmd package
```

macOS / Linux 将 `.\mvnw.cmd` 替换为 `./mvnw`。

## 开发约定

- Controller 只负责参数接收和结果返回，业务逻辑放在 Service。
- 接口权限使用 `@SaCheckPermission`，数据归属和角色限制继续在 Service 中校验。
- 外部服务调用与数据库操作需要设计失败补偿，避免产生残缺文件记录。
- 时间统一使用 `Asia/Shanghai` 时区。
- `Long` 类型 ID 在 JSON 中序列化为字符串，避免前端精度丢失。
- 数据库结构变更必须同步修改 `sql/create_table.sql` 和 `AGENTS.md` 中的结构变更记录。
- 新增数据库或接口功能时应补充对应测试。

## 后续开发

- 成绩录入、查询和统计
- Excel 数据导出与历届资料归档
- 教务系统数据同步和校内通知推送
