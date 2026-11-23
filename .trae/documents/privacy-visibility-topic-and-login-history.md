# 隐私功能改造计划：帖子登录可见、用户私有话题、登录历史

## Context

当前社区所有帖子默认公开展示，匿名用户可查看全部正文与评论；话题只能由管理员在后台创建；用户管理只能看到注册时间与「最近登录时间」，无法审计登录 IP / UA / 入口。

本次改造要解决三件事：

1. 发帖时可选择「登录后可见」，匿名用户只能看到标题等元数据，正文、评论、相关推荐需要登录。
2. 普通用户可申请创建话题，管理员审核通过后创建者自动成为吧主；话题支持公开 / 私有可申请 / 私有隐藏三档可见性，并配套邀请、申请审核、成员管理能力。
3. 后台用户管理可查看某个用户的登录历史（时间、IP、UA、前台/后台入口）。

已与用户确认的决策：

- 「登录可见」帖对匿名用户显示标题 + 锁标识，点击详情展示登录引导，不返回正文与评论。
- 私有话题邀请需要被邀请人确认（站内通知 + 话题页接受/拒绝），不是邀请即加入。
- 私有话题的帖子只在话题详情页内展示；首页、搜索、热门、用户主页、相关问题等站点级列表只展示公开话题帖子。
- 登录历史只记录成功登录，保留 180 天，定时清理。
- 三项功能一次性完整交付；实施时按「批次一 → 批次二」推进并分别验证，不拆成两个发布版本。

## 实施批次

### 批次一：帖子登录可见 + 登录历史

目标：先建立统一的问题访问控制层和登录审计基础，风险最低、可独立验证。

1. 执行 V19 中 `questions.visibility` 与 `user_login_history` 部分。
2. 后端：`Question.visibility`、`QuestionCreateDTO`、`QuestionMapper` 列与 SQL、`QuestionService` 列表过滤、`QuestionAccessService`、`QuestionApiController` 详情/评论/发布守卫、评论/点赞/举报/关注/解锁接口守卫。
3. 后端：登录历史模型、Mapper、Service、登录接入、后台分页接口、180 天清理任务。
4. 前端：发布页可见范围、列表锁标识、详情登录引导、后台登录历史弹窗。
5. 验收：匿名 / 登录 / 作者 / 管理员四类账号验证登录可见帖；前后台登录均产生历史记录。

批次一完成并通过 `mvn test`、`npm run build` 与手工验证后，继续批次二。

### 批次二：用户创建话题与私有话题

目标：在批次一的统一访问控制层上扩展话题审核、可见性、成员与邀请能力。

1. 执行 V19 中 `topics` 字段、`topic_members`、权限点部分。
2. 后端：`TagClass` 字段、话题枚举、`TopicMember` 模型/Mapper、`TopicAccessService`、`TopicMemberService`、`TopicService`、`NotificationTypeEnum` 话题类型、`NotificationServiceImpl` 话题标题装配。
3. 后端：`TopicApiController`、`TopicAdminApiController` 审核接口、`DiscoverApiController` 话题详情/列表/帖子流守卫、`MetaApiController.topicOptions` 按用户过滤、`QuestionApiController.publish` 话题可发帖校验。
4. 前端：`TopicCreateView`、`TopicManageDialog`、`TopicsView`、`TopicDetailView`、`AdminTopicsView`、`MessagesView`、`PublishView` 话题选择提示。
5. 验收：五类账号权限矩阵、私有话题内容不外泄、邀请需接受、管理员审核通过后创建者成为吧主。

## 核心设计

### 三个独立的可见性概念

| 概念 | 字段 | 取值 |
| --- | --- | --- |
| 帖子可见性 | `questions.visibility` | 0 公开 / 1 登录可见 |
| 话题可见性 | `topics.visibility` | 0 公开 / 1 私有可申请（话题信息可见、内容不可见）/ 2 私有隐藏（仅成员/被邀请人可见） |
| 话题审核状态 | `topics.reviewStatus` | 0 待审核 / 1 已通过 / 2 未通过（复用现有审核语义） |

服务端新增统一访问控制层，所有「按问题 ID / 评论 ID / 话题 ID」读取内容的接口必须先经过它，禁止只在列表 SQL 上做过滤：

- `TopicAccessService`：解析话题对当前访问者的可见性、内容权限、成员身份、吧务身份、申请/邀请状态。
- `QuestionAccessService`：在话题权限基础上叠加「登录可见」判断，输出 `canViewContent`、`loginRequired`，供详情、评论、解锁、相关、点赞、举报、关注复用。

### 公开列表统一条件

所有站点级问题查询统一追加：

```sql
exists (
  select 1 from topics t
  where t.id = questions.topicId
    and t.reviewStatus = 1
    and t.status = 1
    and t.visibility = 0
)
```

私有话题的帖子只能通过 `/api/topics/{id}/questions` 在话题页内获取，该接口单独做话题权限校验。

## 阶段一：数据库迁移

新增 `src/main/resources/db/migration/V19__content_visibility_topic_membership_login_history.sql`（禁止修改 V1~V18）：

### questions

- `visibility tinyint NOT NULL DEFAULT 0 COMMENT '0 公开，1 登录可见'`
- 索引 `idx_questions_visibility (visibility, status, reviewStatus)`

### topics

- `visibility tinyint NOT NULL DEFAULT 0 COMMENT '0 公开，1 私有可申请，2 私有隐藏'`
- `createdBy bigint NULL COMMENT '创建人 userId，存量话题为 NULL'`
- `reviewStatus tinyint NOT NULL DEFAULT 1 COMMENT '0 待审核，1 已通过，2 未通过'`
- `reviewReason varchar(200) NULL`
- `reviewerId bigint NULL`
- `reviewTime bigint NULL`
- 索引 `idx_topics_review (reviewStatus, status, visibility, type)`、`idx_topics_creator (createdBy, id)`

存量话题默认公开 + 已通过，行为不变。

### topic_members

```sql
id bigint auto_increment
topicId bigint NOT NULL
userId bigint NOT NULL
status tinyint NOT NULL DEFAULT 0 COMMENT '0 待审核/待接受，1 已加入，2 已拒绝/已取消'
joinType varchar(16) NOT NULL DEFAULT 'apply' COMMENT 'apply 申请，invite 邀请'
message varchar(200) NULL
reviewerId bigint NULL
reviewTime bigint NULL
createTime bigint NOT NULL
UNIQUE KEY uk_topic_members (topicId, userId)
KEY idx_topic_members_topic (topicId, status, joinType)
KEY idx_topic_members_user (userId, status)
外键：topicId -> topics.id、userId -> users.id，均级联删除
```

吧主/小吧不写入 `topic_members`，统一以 `topic_moderators` 为唯一来源，避免双写不一致。

### user_login_history

```sql
id bigint auto_increment
userId bigint NOT NULL
loginTime bigint NOT NULL
ip varchar(64) NOT NULL DEFAULT ''
userAgent varchar(512) NULL
loginType tinyint NOT NULL DEFAULT 1 COMMENT '1 前台，2 后台'
KEY idx_login_history_user (userId, loginTime, id)
KEY idx_login_history_time (loginTime)
外键 userId -> users.id 级联删除
```

### 权限点

- 新增 `content:topic:create`（社区），写入 `sys_permissions`。
- 授予内置角色 `user`（社区成员）与 `super_admin`。
- 同步 `PermissionCodes.CONTENT_TOPIC_CREATE`。
- 话题审核沿用已有 `admin:topic`。

## 阶段二：后端模型与基础设施

### 模型 / 枚举 / DTO

- `Question` 增加 `visibility`；`QuestionDto` 自动透出。
- `TagClass` 增加 `visibility`、`createdBy`、`reviewStatus`、`reviewReason`、`reviewerId`、`reviewTime`，并增加仅后台展示的 `@Column(ignore = true) creatorName`。
- 新增 `TopicMember`、`UserLoginHistory` 模型及 Mapper。
- 新增枚举：
  - `QuestionVisibilityEnum`：PUBLIC / LOGIN_REQUIRED。
  - `TopicVisibilityEnum`：PUBLIC / PRIVATE_APPLY / PRIVATE_HIDDEN。
  - `TopicMemberStatusEnum`、`TopicJoinTypeEnum`。
- 新增 DTO：
  - `TopicCreateDTO`（标题、分类、简介、图标、可见性、验证码）
  - `TopicSettingsDTO`（简介、图标、可见性）
  - `TopicJoinDTO`（申请留言）
  - `TopicInviteDTO`（用户登录账号）
  - `TopicReviewApplicationDTO`（approve、reason）
  - `TopicMemberDto`（成员信息 + status + joinType + message + createTime）
  - `AdminTopicReviewDTO`（approved、reason）
- `QuestionCreateDTO` 增加 `visibility`（0/1，声明式校验）。
- `NotificationTypeEnum` 新增话题通知类型 22~27：
  - 22 话题审核通过、23 话题审核未通过
  - 24 收到加入申请（发给吧主团队）
  - 25 申请通过、26 申请未通过
  - 27 收到话题邀请
  - 增加 `isTopicType(int)` 静态判断，供通知装配使用。

### Mapper 改造

`QuestionMapper`：

- `LIST_COLUMNS` / `LIST_QUERY_COLUMNS` / `REVIEW_QUERY_COLUMNS` 增加 `visibility`。
- `createQuestion` 显式 INSERT 增加 `visibility`；`updateQuestion` 更新 `visibility`。
- 增加统一的公开话题过滤片段，应用到：
  - `fulltextSearchQuestion` / `fulltextSearchQuestionCount`
  - `getRelevantQuestion`
  - `getQuestionsByAlterTimeCursor` / `getNoCommentQuestionsByCursor`
  - `countQuestionByUserIds`
  - `QuestionService.listCondition`、`getQuestionByUserId`、`getQuestionListForTag`、`selectQuestionDtosByIds`
- `selectTopicQuestions` / `countTopicQuestions` 保持不加过滤，因为调用方必须先通过话题权限校验。
- `TagMapper.countPublicUsage` 增加公开话题过滤。

`TagClassMapper`：

- `updateTagClass` 支持更新 `visibility`。
- `selectAdminList` 增加 `reviewStatus`、`visibility` 过滤。
- 新增按创建人查询、按审核状态计数。

新增 `TopicMemberMapper`：

- `selectOne(topicId, userId)`、`selectByTopicAndStatus`、`pageByTopic`、`countByTopic`
- `upsert` / `updateStatus`（带 `where status = 0` 的 CAS）
- `deleteByTopicAndUser`、`selectTopicIdsByUser`

新增 `UserLoginHistoryMapper`：

- `insert`、`pageByUser`、`deleteBefore(loginTime)`

### 服务层

新增 `TopicAccessService`：

- `TopicAccess resolve(long topicId, User viewer)`，返回：
  - `topic`、`visible`、`canViewInfo`、`canViewContent`
  - `isMember`、`isOwner`、`isModerator`、`isAdmin`
  - `joinStatus`（none / pending / joined / rejected / invited / public）
- 规则：
  - 待审核 / 未通过话题：仅创建者与 `admin:topic` 可见。
  - 公开话题：所有人可见、可看内容。
  - 私有可申请话题：所有人可见话题信息；内容仅成员 / 吧务 / 创建者 / 管理员可见；匿名与非成员看到锁定态。
  - 私有隐藏话题：非成员、非吧务、非创建者、非管理员一律 404；待接受邀请者可看到话题信息以完成接受/拒绝。
  - 全局 `admin:topic` / `admin:question` / `admin:access` 拥有后台绕过权限。
- `requireContentAccess(topicId, viewer)`：私有可申请抛 403，私有隐藏抛 404，避免泄露存在性。
- `canPostInTopic(topicId, user)`：公开话题全部登录用户可发；私有话题仅成员 / 吧务 / 创建者 / 管理员可发。

新增 `QuestionAccessService`：

- `QuestionAccess resolve(Question question, User viewer, User admin)`，在话题权限之上叠加：
  - `canViewContent`
  - `loginRequired`（`visibility = 1` 且未登录）
  - 帖子作者可访问自己的帖子；管理员可访问全部。
- 所有按问题读取内容的入口统一调用，禁止各控制器重复拼装规则。

新增 `TopicMemberService`：

- `apply(topicId, message, user)`：
  - 仅审核通过的话题；公开话题拒绝申请；私有隐藏话题提示仅限邀请。
  - 已是成员 / 已有待审申请 / 已收到邀请分别给出明确冲突提示。
  - 被拒绝后可重新申请（CAS 重置为待审）。
  - 唯一键兜底 + `DuplicateKeyException` 捕获。
- `invite(topicId, userCode, operator)`：吧务 / 吧主 / 管理员可邀请；生成 `status=0, joinType=invite` 记录并通知被邀请人。
- `acceptInvite` / `declineInvite`：仅被邀请人本人、仅待接受邀请；接受后 `status=1`。
- `reviewApplication(topicId, memberId, approve, reason, operator)`：吧务 / 吧主 / 管理员；仅待审申请；CAS 更新并通知申请人。
- `listMembers(topicId, status, page, size)`：吧务 / 吧主 / 管理员；批量装配用户头像与名称。
- `removeMember(topicId, userId, operator)`：仅吧主 / 管理员；删除成员或待处理邀请。
- `isMember` / `isInvited` / `joinStatus` 供访问控制层复用。

新增 `TopicService`（或并入 `TagClassService`，建议独立避免职责膨胀）：

- `applyTopic(TopicCreateDTO, User, HttpServletRequest)`：校验验证码、分类、标题重复；写入 `status=0, reviewStatus=0, createdBy`，默认图标；校验可见性取值。
- `mine(User)`：返回我创建的话题（含审核状态）与我加入的话题。
- `updateSettings(topicId, TopicSettingsDTO, User)`：仅吧主 / 管理员；可改简介、图标、可见性。
- 缓存同步：创建 / 审核 / 改可见性 / 删除后刷新 `TagClassCache`，并失效 `SiteCacheService.TOPICS`、`TOPICS + "options"`、`META + "tagClasses"`、`HOME`、`META + "hots"`、`META + "hotQuestions"`。

扩展 `TagClassService`：

- `findApprovedById`：只返回 `status=1 && reviewStatus=1`。
- `getTagClassByTypeNotUserPage` 过滤 `reviewStatus=1 && status=1 && visibility != 2`。
- `searchEnabledTopics(keyword, limit, userId)`：公开话题 + 当前用户已加入 / 吧务 / 创建者的私有话题。

扩展 `TagClassCache`：

- 保留全量缓存供管理员与鉴权后读取。
- 增加 `getApprovedById`、`getPublicList`，公开接口禁止直接使用无过滤的全量方法。

扩展 `NotificationServiceImpl`：

- 对 `NotificationTypeEnum.isTopicType(type)` 的通知，`outerId` 按话题 ID 批量查询 `topics` 并填充 `outerTitle`；话题已删除时显示「话题已删除」。
- 前端 `MessagesView` 对应跳转 `/topics/{outerId}`。

新增 `UserLoginHistoryService`：

- `record(userId, ip, userAgent, loginType)`：best-effort，失败只记录告警，不能阻断登录。
- `pageByUser(userId, page, size)`。
- 定时清理 180 天前记录（新增 `schedule/LoginHistoryCleanupTask`，使用 `RedisSchedulerLock`，每天凌晨执行）。

### 控制器与接口契约

新增 `TopicApiController`（前台）：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/topics` | `content:topic:create` | 申请创建话题，含验证码 |
| GET | `/api/topics/mine` | 登录 | 我创建的 / 我加入的话题 |
| POST | `/api/topics/{id}/join` | 登录 | 申请加入（私有可申请） |
| POST | `/api/topics/{id}/invites/accept` | 登录 | 接受邀请 |
| POST | `/api/topics/{id}/invites/decline` | 登录 | 拒绝邀请 |
| GET | `/api/topics/{id}/members` | 吧务/吧主/管理员 | 成员与待审申请分页 |
| POST | `/api/topics/{id}/invites` | 吧务/吧主/管理员 | 邀请用户 |
| POST | `/api/topics/{id}/applications/{memberId}/review` | 吧务/吧主/管理员 | 通过/拒绝申请 |
| POST | `/api/topics/{id}/members/remove` | 吧主/管理员 | 移除成员 |
| POST | `/api/topics/{id}/settings` | 吧主/管理员 | 修改简介/图标/可见性 |

管理员接口：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/admin/topics?reviewStatus=&visibility=&keyword=&type=` | `admin:topic` | 话题列表含创建人、审核状态 |
| POST | `/api/admin/topics/{id}/review` | `admin:topic` | 通过/驳回，通过时幂等写入 owner |
| GET | `/api/admin/users/{id}/login-history?page=&size=` | `admin:user` | 登录历史分页 |

`DiscoverApiController` 改造：

- `/api/topics/{id}`：使用 `TopicAccessService`；不可见返回 404，私有可申请返回 200 + `access.canViewContent=false`；补充 `access`、`pendingApplicationCount`。
- `/api/topics/{id}/questions`：先 `requireContentAccess`。
- `/api/topics`：过滤待审、停用、私有隐藏；私有可申请保留并带可见性字段。
- `/api/search` 继续走 `QuestionService`，由统一的公开话题 SQL 过滤。

`QuestionApiController` 改造：

- `detail`：
  - 先做话题访问校验；作者 / 管理员可看自己的帖子。
  - 私有可申请非成员返回 403 内容锁定；私有隐藏非成员返回 404。
  - 登录可见 + 匿名：返回问题元数据，`contents` 为空，`loginRequired=true`，不累加浏览数。
  - 正常可见时才加载正文、标签、点赞/关注状态。
- `comments`、`relevant`、`unlock`：统一走 `QuestionAccessService`，登录可见匿名请求返回 403 登录提示。
- `publish`：校验话题 `status=1 && reviewStatus=1`，并调用 `canPostInTopic`；写入 `visibility`。
- `edit`：返回 `visibility`，保持作者所有权校验。

其他必须加守卫的接口：

- `CommentApiController`：创建评论前校验问题可访问；`twoLevelComment` 需由 commentId 反查 questionId 后鉴权，避免枚举评论 ID 绕过。
- `ClickLikeApiController`、`ReportApiController`、`FollowApiController.followQuestion`：先校验问题可访问。
- `FollowApiController.followTopic`：私有隐藏话题非成员按 404 处理。
- `MetaApiController.topicOptions`：传入当前用户，只返回可发帖话题。
- `IndexTopQuestion` / `HotQuestionTasks` / `HotUserTasks`：依赖改造后的公开列表查询，自动排除私有话题内容。
- `HomeApiController`：公共缓存只包含公开话题数据，无需按用户区分。

### 登录历史接入

- `SignInServiceImpl.signIn` 成功后调用 `UserLoginHistoryService.record(...)`（`loginType=1`）。
- `AdminApiController.adminLogin` 成功后调用 `record(...)`（`loginType=2`），并同步更新 `users.lastTime`。
- IP 统一使用 `IpUtil.getIpAddr(request)`；UA 取 `User-Agent` 并截断到 512 字符。
- 登录历史写入失败只记录日志，不影响登录结果。

## 阶段三：前端改造

### 全局 API / 路由

- `web/src/api/index.js`：
  - `discoverApi` 增加话题创建、我的话题、申请加入、邀请接受/拒绝、成员列表、邀请、申请审核、移除成员、话题设置。
  - `adminApi` 增加话题审核、登录历史。
- `web/src/router/index.js`：
  - 新增 `/topics/create`，`requiresAuth: true`，`permission: 'content:topic:create'`。

### 新建页面 / 组件

- `web/src/views/TopicCreateView.vue`：话题创建表单（名称、分类、简介、图标上传、可见性、验证码），提交后提示「已提交审核」并跳转话题页或我的话题。
- `web/src/components/TopicManageDialog.vue`：吧主/小吧管理弹窗，含：
  - 待审申请列表（通过/拒绝、申请留言）
  - 成员列表（移除）
  - 邀请用户（按登录账号）
  - 话题设置（仅吧主/管理员）：可见性、简介、图标

### 现有页面改造

- `PublishView.vue`：新增「可见范围」选择（公开 / 登录后可见），编辑时回显；提交带 `visibility`。
- `QuestionList.vue`：`visibility === 1` 显示锁标识「登录可见」。
- `QuestionView.vue`：
  - 读取 `loginRequired`；为 true 时不加载正文、评论、相关问题。
  - 正文区域替换为登录引导卡片（保留标题、作者、话题等元数据）。
  - 登录后跳回当前详情页。
- `TopicsView.vue`：
  - 顶部增加「发起话题」「我的话题」入口。
  - 话题卡片显示「私有 · 可申请」徽章；私有隐藏话题不进入公共列表。
- `TopicDetailView.vue`：
  - 使用新接口返回的 `access`：
    - `canViewContent=false` 时展示锁定面板：申请加入 / 申请审核中 / 被拒绝可重新申请 / 仅限邀请 / 接受邀请。
    - 无内容权限时不请求帖子列表。
  - 吧主/小吧显示「话题管理」按钮，打开 `TopicManageDialog`。
  - 创建者可见待审 / 驳回横幅与驳回原因。
- `MessagesView.vue`：话题通知类型跳转 `/topics/{outerId}`。
- `AdminTopicsView.vue`：
  - 增加审核状态筛选。
  - 列表展示创建人、可见性、审核状态。
  - 待审话题提供「通过 / 驳回」操作与驳回原因输入。
  - 编辑弹窗增加可见性选项。
- `AdminUsersView.vue`：
  - 操作菜单增加「登录历史」。
  - 弹窗展示分页表格：登录时间、IP、入口（前台/后台）、UA（友好解析 + 原文 title）。
  - 可复用 `Pagination`、`formatDateTime`；UA 解析放在 `utils/format.js`。

## 权限矩阵

| 操作 | 匿名 | 登录非成员 | 成员 | 吧务 | 吧主 | 管理员 |
| --- | --- | --- | --- | --- | --- | --- |
| 公开话题信息 / 内容 | 是 | 是 | 是 | 是 | 是 | 是 |
| 私有可申请话题信息 | 是 | 是 | 是 | 是 | 是 | 是 |
| 私有可申请话题内容 | 否 | 否 | 是 | 是 | 是 | 是 |
| 私有隐藏话题信息 / 内容 | 否 | 否 | 是 | 是 | 是 | 是 |
| 创建话题 | 否 | 是 | 是 | 是 | 是 | 是 |
| 申请加入 | 否 | 是 | 否 | 否 | 否 | 否 |
| 审核申请 / 邀请 / 移除成员 | 否 | 否 | 否 | 是 | 是 | 是 |
| 修改话题可见性 | 否 | 否 | 否 | 否 | 是 | 是 |
| 话题审核 | 否 | 否 | 否 | 否 | 否 | `admin:topic` |
| 查看登录历史 | 否 | 否 | 否 | 否 | 否 | `admin:user` |

## 缓存与安全要点

- 话题创建 / 审核 / 可见性变更 / 停用 / 删除：刷新话题缓存 + 失效首页、话题列表、话题选项、热门缓存。
- 帖子可见性变更：失效首页、热门问题缓存。
- 成员关系变化不刷新话题缓存，访问控制实时查 `topic_members` / `topic_moderators`。
- `TagClassCache` 继续全量缓存，但公开接口只允许调用过滤后的方法；`getById` 仅用于鉴权后读取。
- 申请 / 邀请并发依赖唯一键 + CAS，捕获 `DuplicateKeyException`，避免覆盖已加入状态。
- 管理员审核话题通过时，在事务内幂等写入 `topic_moderators` 的 owner；管理员后台再任命 owner 时先降级旧 owner，禁止移除最后一位 owner。
- 通知统一按类型识别话题通知，避免 `outerId` 被误按问题 ID 查询导致标题串号。
- 登录历史不写入密码、token；UA 截断；IP 使用可信代理解析工具。

## 测试与验证

### 自动化

- 同步修改受构造参数影响的测试：`SignInServiceImplTest`（新增 `UserLoginHistoryService`）、`NotificationServiceImplTest`（如新增 `TagClassMapper`）。
- 新增单元测试：
  - `TopicAccessServiceImplTest`：匿名 / 非成员 / 成员 / 吧务 / 吧主 / 管理员 × 公开 / 私有可申请 / 私有隐藏矩阵。
  - `TopicMemberServiceImplTest`：申请、重复申请、邀请、接受、拒绝、审核通过与并发 CAS。
  - `QuestionAccessServiceImplTest`：登录可见匿名 / 登录用户 / 作者 / 管理员。
  - 登录历史 best-effort：写入异常不影响登录成功。
- 执行：
  - 后端 `mvn test`
  - 前端 `cd web; npm run build`

### 手工矩阵验证

准备 5 类账号：匿名、普通非成员、私有话题成员、吧务、管理员。

1. 匿名首页 / 搜索 / 用户主页 / 相关问题看不到私有话题帖子；公开列表可见「登录可见」标题与锁标识。
2. 匿名打开登录可见帖：只返回标题等元数据，正文、评论接口均 403；登录后可正常查看。
3. 私有可申请话题：非成员可打开话题页但内容锁定，可提交申请；吧主在管理弹窗通过后，成员可查看内容。
4. 私有隐藏话题：非成员直接访问话题 URL / 帖子 URL / 评论 URL 均 404；吧主邀请后，被邀请人在通知中进入话题并接受，随后可见内容。
5. 匿名 / 非成员直接请求评论、二级评论、阅读密码解锁、点赞、举报、关注接口，均不能绕过话题权限。
6. 管理员后台审核话题通过后，创建者立即成为吧主并可管理申请；驳回时收到原因通知。
7. 后台用户管理打开登录历史，前台与后台登录均产生记录，展示时间、IP、入口、UA；180 天前记录由定时任务清理。

## 主要涉及文件

后端：

- `src/main/resources/db/migration/V19__content_visibility_topic_membership_login_history.sql`
- `src/main/java/com/buguagaoshu/community/model/Question.java`、`TagClass.java`、新增 `TopicMember.java`、`UserLoginHistory.java`
- `src/main/java/com/buguagaoshu/community/mapper/QuestionMapper.java`、`TagClassMapper.java`、新增 `TopicMemberMapper.java`、`UserLoginHistoryMapper.java`
- `src/main/java/com/buguagaoshu/community/service/impl/QuestionServiceImpl.java`、`TagClassServiceImpl.java`、`NotificationServiceImpl.java`、`SignInServiceImpl.java`
- 新增 `TopicAccessService`、`QuestionAccessService`、`TopicMemberService`、`TopicService`、`UserLoginHistoryService` 及实现
- `src/main/java/com/buguagaoshu/community/controller/api/QuestionApiController.java`、`CommentApiController.java`、`DiscoverApiController.java`、`FollowApiController.java`、`ClickLikeApiController.java`、`ReportApiController.java`、`MetaApiController.java`、`AdminApiController.java`、`AdminPageApiController.java`、`TopicAdminApiController.java`、新增 `TopicApiController.java`
- `src/main/java/com/buguagaoshu/community/enums/NotificationTypeEnum.java`、新增可见性与成员枚举
- `src/main/java/com/buguagaoshu/community/schedule/` 新增登录历史清理任务

前端：

- `web/src/api/index.js`、`web/src/router/index.js`
- 新增 `web/src/views/TopicCreateView.vue`、`web/src/components/TopicManageDialog.vue`
- `web/src/views/PublishView.vue`、`QuestionView.vue`、`TopicsView.vue`、`TopicDetailView.vue`、`MessagesView.vue`
- `web/src/components/QuestionList.vue`
- `web/src/views/admin/AdminTopicsView.vue`、`AdminUsersView.vue`
- `web/src/utils/format.js`
