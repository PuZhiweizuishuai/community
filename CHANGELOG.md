# 更新日志（Changelog）

本文件记录「不挂高数社区」的更新历史。原 `README.md` 中的「更新日志」章节已整体迁移至此，并补充 2026-11-15 的后端全量升级记录。

按时间倒序排列。

---

## 2026-11-15 · 修复：日间模式代码块外圈黑框

- **根因**：`main.css` 里 `.markdown-body pre` 的 `background` 被写死为 `#0f172a`（深色），而日间模式下 `code.hljs` 由 `github.min.css` 提供 `#fff` 白底；`pre` 的 16px 内边距加圆角就形成一圈深色「黑框」包住白色代码区。夜间两者都是深色，所以此前没暴露。
- **修复**：
  - 抽出 `--code-block-bg` / `--code-block-color` 两个 CSS 变量：白天 `#ffffff` / `#24292e`（与 github 亮色主题一致），夜间 `#0b1120` / `#c9d1d9`（与 github-dark 一致）。
  - `.markdown-body pre` 改用变量并加 `1px` 浅色边框（白天 `#e2e8f0`、夜间 `#334155`）区分代码块与正文。
  - 新增 `.markdown-body pre > code, .markdown-body pre > code.hljs` 规则（`background: transparent`、`color: inherit`、`padding: 0`），消除 Vditor 给 `code.hljs` 单独设置的背景与二次内边距；底色与留白统一由 `pre` 提供。
  - 清理夜间兜底块里与上述规则冲突的 `padding: 1em` / `padding: 3px 5px` 覆盖，并改用变量。
- **验证**：日间 `pre=rgb(255,255,255)`、`code=transparent`、边框 `rgb(226,232,240)`、关键字 `rgb(215,58,73)`，截图确认黑框消失；夜间 `pre` 与 `code` 均为 `rgb(11,17,32)`、文字 `rgb(201,209,217)`，暗色高亮保持正常。build / oxlint / eslint 通过，控制台 0 错误。
- 备注：截图中代码块右下角的白色小胶囊是 Vite 的 `vue-devtools__panel` 悬浮按钮（仅开发环境），不是代码块样式。

---

## 2026-11-15 · 修复：Vditor 代码块夜间模式

### 根因

`Vditor.preview` / 编辑器的 `preview.mode` 只影响渲染模式（mermaid 等），**不会切换代码高亮主题**。代码高亮样式是运行时由 `highlightRender` 以 `<link id="vditorHljsStyle">` 从 CDN 注入的，默认固定为 `github.min.css`（亮色），内容主题同理固定 `light.css`。因此夜间模式下 `code.hljs` 仍是 `background:#fff; color:#24292e`，白底深字，和深色页面割裂。

### 修复

- 新增 `web/src/utils/vditorTheme.js`：从 `vditor/package.json` 读取版本号生成 CDN 路径，集中定义白天 / 夜间三套主题（编辑器 `classic|dark`、内容 `light|dark`、代码高亮 `github|github-dark`、预览 `light|dark`），导出 `vditorPreviewTheme(isDark)`。
- `MarkdownView.vue`：渲染时展开 `vditorPreviewTheme()`，并显式传 `mode`；主题变化仍会重新渲染。
- `VditorEditor.vue`：初始化 `preview` 时带上 `hljs.style` / `theme.current` / `theme.path`；主题切换的 watcher 改为 `editor.setTheme(editorTheme, contentTheme, codeTheme)`，一次同步编辑器外观、内容主题与代码高亮。
- `main.css`：加入 `[data-theme='dark']` 作用域下的 GitHub Dark token 配色兜底（`.hljs`、`.hljs-keyword`、`.hljs-string` 等），选择器优先级高于 Vditor 动态注入的 CDN 样式；即使 CDN 样式加载失败，代码块仍是暗色。

### 验证（浏览器实测）

- 夜间：`<link>` 指向 `github-dark.min.css` 与 `content-theme/dark.css`，`pre/code` 背景 `rgb(11,17,32)`、文字 `rgb(201,209,217)`、关键字 `rgb(255,123,114)`。
- 白天：自动切回 `github.min.css` / `light.css`，代码块恢复白底深字。
- 编辑器在页面上实时切换主题也能同步更新 `.vditor--dark` 与高亮样式；手动移除 Vditor 注入的两个 `<link>` 后，本地兜底规则仍保持暗色代码块。
- `npm run build`、oxlint、eslint 通过，浏览器控制台 0 错误。

---

## 2026-11-15 · 交互细节、投放批处理、搜索历史与缓存/数据库优化（第四轮）

### 缺陷修复

- **聊天室消息不可见**：气泡同时写了 `bg-white text-slate-700` 与条件类 `bg-indigo-600 text-white`，Tailwind 工具类顺序导致「白底白字」。改为互斥类名 `chat-bubble-mine / chat-bubble-other`，并在 `main.css` 用变量定义两套气泡（兼容夜间模式）。实测发送后 `bg=rgb(79,70,229)` / `color=rgb(255,255,255)`，文字正常显示。

### 前端体验

- **登录 / 注册页**按主流认证页重做：新增 `AuthShell.vue` 左侧品牌展示区（渐变、价值点、社区数据）+ 右侧表单卡；登录页增加邮箱/密码即时校验、密码显隐、页内错误横幅、记住我、忘记密码入口；注册页分「账号信息 / 安全设置 / 补充资料」三段、密码强度条、账号与邮箱占用校验、错误横幅。两页均支持响应式与主题切换。
- **搜索历史**：新增 `utils/searchHistory.js`（localStorage `community-search-history`，最多 10 条、大小写去重、最近优先、可单条删除/清空）。导航搜索框聚焦时弹出历史下拉，搜索页在搜索框下方展示历史 chips，直接访问搜索 URL 也会记录。
- **广告投放页**：列表新增多选、全选、批量投放 / 批量结束、位置与状态筛选、各位置容量进度条、逐条结果回执弹窗；勾选后显示批量操作条。

### 后端：广告投放逻辑

- 新增 `AdvertisementBatchDTO` / `AdvertisementOperationResult`；`AdvertisementServiceImpl` 重写投放核心：位置白名单、天数 1~365、只统计有效期内广告的容量、续期不重复占名额、改位置时先从旧位置缓存移除、`synchronized` 保证批量容量判断不被并发绕过、每条独立回执。
- 新接口 `GET /api/admin/advertisement/positions`（容量/已用/剩余）、`POST /api/admin/advertisement/batch-setting`、`POST /api/admin/advertisement/batch-close`；旧的单条 setting/close 复用批量逻辑并回传具体原因。编辑广告时同步刷新正在投放的缓存对象。
- 实测：非法位置 / 空列表被拒绝；3 个 ID（含重复）批量投放返回 2 条成功并正确累加容量；批量结束 2 条成功。测试数据已还原。

### 后端：缓存模式

- 新增 `SiteCacheService`：基于 `ConcurrentHashMap` 的本地 TTL 缓存，提供 `get(key, ttl, loader)`、按前缀失效、命中/未命中统计、每分钟过期清理。
- `/api/meta` 的热门标签、热门问题、活跃用户、标签字典、话题字典、动态分类、在线人数全部走缓存；`/api/home` 按查询条件缓存公共列表 20 秒（`followTag` 与用户相关，仍逐请求计算）；`/api/topics` 按分类缓存 60 秒。
- 发布/编辑问题、发表评论、后台改话题/分类、手动重算榜单都会主动失效对应前缀。新增 `/api/admin/cache`（命中率/条目）、`/refresh`、`/invalidate`；后台「控制面板 → 缓存与统计」展示命中率并可一键清空。实测稳定在 75%~80% 命中率。

### 数据库优化与升级脚本

- `databases.sql` 末尾补「全新安装性能优化」段落：统一 InnoDB + `utf8mb4_unicode_ci`，新增 20 个复合/覆盖索引（问题、评论、通知、点赞、关注、话题、广告、在线用户、管理数据），并 `ANALYZE TABLE`。
- 新增 `SQL_Upgrade/001_performance_and_consistency.sql`（幂等）：字符集/引擎统一、通过 `information_schema` 判断的幂等建索引存储过程、动态分类表、废弃显示宽度与超大 URL 字段收敛、统计信息更新；附带验证用 `EXPLAIN` 说明。
- 新增 `SQL_Upgrade/001_rollback.sql`：幂等移除本轮新增的全部索引（不删除业务表）。
- 用 MySQL JDBC 驱动在测试库实际执行验证：**升级 47 条语句 0 失败 → 重复执行 0 失败 → 回滚 24 条 0 失败 → 再次升级 0 失败**。过程中修正两处索引长度问题（`topic.title` 不能用 191 前缀，因为列只有 100；`Questions.tag` 必须用 191 前缀，否则 utf8mb4 下整列 1024 字节超过 767 上限）。

### 验证

- `mvn -q -DskipTests package spring-boot:run` 启动成功；`npm run build`、oxlint、eslint 通过。
- 浏览器实测：聊天消息可见、登录/注册新布局（品牌分栏 + 3 段表单）、广告页 21 个复选框 / 9 个容量条、缓存面板（9 条目 / 78.5% 命中率）、搜索历史写入与展示，控制台 0 错误。

---

## 2026-11-15 · 品牌视觉、主题、算法与分类动态化（第三轮）

### 品牌与视觉

- `static/favicon.png`（748×979 / 524KB）用 Java ImageIO 等比压缩为 `web/public/logo.png`（160×160 透明 PNG / 26KB，约减少 95%），新增 `SiteLogo.vue`，替换导航、登录、注册、后台登录与后台侧栏的「不」文字占位符；`index.html` 的 favicon 与标题同步更新。
- **后台广告编辑页**重做：面包屑标题栏 + 左侧「基础信息 / 展示素材」两张分区卡（拖拽上传、实时大图预览、URL 兜底）+ 右侧粘性「前台效果预览 / 保存前检查 / 操作 / 小贴士」。
- **页面切换动画**：`App.vue` 三种布局分支统一使用 `<Transition name="page" mode="out-in" appear>`，按 `route.path` 做 key（查询串变化如翻页不触发整页动画）；CSS 用淡入 + 10px 上移、离开 0.16s / 进入 0.28s，并尊重 `prefers-reduced-motion`。

### 夜间模式

- 新增 `stores/theme.js`：`mode = system | light | dark`，默认跟随 `prefers-color-scheme` 并监听系统变化；手动切换写入 `localStorage('community-theme-mode')`，`index.html` 内联脚本在首屏渲染前设定 `data-theme` 避免闪屏。
- `main.css` 抽出 `--page-bg / --surface-bg / --surface-border / --text-*` 等变量，并补齐夜间模式对 `text-slate-* / bg-white / bg-slate-* / border-slate-*` 等显式工具类的覆盖；Vditor 编辑器与 Markdown 预览跟随主题切换（编辑器 `setTheme`、预览 `mode: light|dark`）。
- `ThemeToggle.vue` 提供「跟随系统 / 白天 / 夜间」三选一下拉，前台导航与后台顶栏均可切换。

### 热门算法（后端）

- 新增 `util/HotRankingUtil.java`：多信号加权（评论 10 > 点赞 6 > 关注 4 > 浏览 1）、`1 + log1p` 对数抑制、按「最后活跃时间」指数衰减 + 衰减下限、按「发布时间」gravity 分母，分数放大 100 万倍存 long。候选池 100 → 300，剔除已删除问题。
- `HotQuestionTasks` / `HotTagTasks` 改用新打分；标签额外做 trim、大小写归一去重、空标签过滤、单篇内重复只计一次、汇总分数二次对数压缩。
- `HotQuestionCache` / `HotTagCache` 去掉不稳定的 PriorityQueue，改为确定性排序 + 去重 + TopN 截取。实测旧数据下热门问题分数从全 0 变为 24258 / 24099 … 的清晰梯度。

### 聊天室单窗口限制

- 新增 `ChatPresenceRegistry`（`ConcurrentHashMap<userId, Presence>`），`ChatController.addUser` JOIN 时占用席位；同一账号第二个窗口返回带 `clientToken` 的 `DUPLICATE` 消息并拒绝 JOIN，`sendMessage` 校验令牌防止绕前端发消息。
- `ChatMessage` 增加 `clientToken` 与 `MessageType.DUPLICATE`；`WebSocketEventListener` 断开时按令牌精确释放席位并广播 LEAVE，去掉不可靠的静态 `onlineCount++`。
- 前端每个聊天窗口生成一次性 `clientToken`，只处理属于自己的拒绝提示、显示醒目错误横幅并主动断开；开启 10s STOMP 心跳。实测第二个窗口被拒绝、关闭第一个窗口后可重新加入。

### 分类动态化（前后端）

- 新增 `classification` 表（`kind=question|topic`、`code`、`name`、`sortOrder`、`status`）与 `ClassificationSchemaInitializer`：老库自动 `CREATE TABLE IF NOT EXISTS` 并用原枚举值播种（6 个问题分类 + 7 个话题分类），新库可直接用 `databases.sql` 的 DDL。
- 新增 `Classification` / `ClassificationMapper` / `ClassificationCache` / `ClassificationAdminApiController`（`/api/admin/classifications` 的 list / save / delete / refresh）。`/api/meta` 的 `classifications` 与 `topicTypes` 改读动态分类（缓存为空时回退枚举）。
- `QuestionServiceImpl` 把 `class` 编号 → 分类名的映射从写死枚举改为读动态缓存；`TagClassServiceImpl` 不再限制类型 ≤ 7；`TopicAdminApiController` 校验分类编号必须存在于启用列表。
- 重命名问题分类时同步 `Questions.classification`；修改话题分类编号时同步 `topic.type`；删除分类前校验关联问题数（问题分类按名称、话题分类按该分类下话题的标签用 `FIND_IN_SET` 统计），有问题则 409 拒绝并提示数量，话题分类还会校验是否残留话题。
- 前端新增 `/admin/classifications` 分类管理页（双 Tab、占用数展示、增删改、删除错误页内提示、保存后刷新前台 meta）；`AdminTopicsView` 改为**按所属分类分组展示**（可折叠、显示话题数与讨论数、组内新增、下拉直接改分类）+ 平铺视图切换。

### 验证与稳定性

- `main.js` 增加 `app.config.errorHandler`，统一兜底异步 watcher 中的请求异常（后端短暂不可达时不再出现 Vue 未处理拒绝警告）。
- `mvn -q -DskipTests package spring-boot:run` 启动成功，分类表自动建表并播种；`/api/meta` 返回 7 个问题分类 + 7 个话题分类；分类新增 / 重命名 / 重名 409 / 删除 409（「提问」1 条、「开发语言」22 条）/ 无占用删除成功均验证通过。
- `npm run build`、oxlint、eslint 通过；浏览器实测主题切换（light → dark、surface `#fff` → `#1e293b`、刷新后仍为 dark）、Logo、广告编辑页、分类管理页（6 行）、话题分组页（7 组 / 57 卡片）、聊天室双窗口互踢，控制台 0 错误。

---

## 2026-11-15 · 前端体验优化与缺陷修复（第二轮）

### 新增：后台话题分类编辑

- `TagClassMapper` 补齐 `selectTagClassById`、`insertTagClass`（回填自增 id）、`updateTagClass`、`deleteTagClassById`。
- 新增 `TopicAdminApiController`：`GET /api/admin/topics`（按分类/关键字过滤）、`POST /api/admin/topics/save`（新增或编辑，标题去重、分类 1~7 校验、统计值保持、保存后立即调用 `TagClassTasks.TagClassCache()` 刷新缓存）、`POST /api/admin/topics/delete`、`POST /api/admin/topics/refresh`。全部要求管理员会话。
- `FileApiController` 图片上传同时接受 `session.user` 与 `session.admin`，后台编辑话题图标时可直接上传；`type=top` 只在真实前台用户会话下回写封面字段。
- `TagClassServiceImpl.getTagClassByTypeNotUserPage` 增加缓存未初始化时的数据库兜底，避免首屏 NPE。
- 前端新增 `views/admin/AdminTopicsView.vue`（卡片列表、分类/关键字筛选、编辑弹窗、启停、删除、图标上传、同步缓存）、路由 `/admin/topics` 与侧栏菜单；新增 `static/image/topicicon/default.svg` 默认图标。

### 缺陷修复

- **个人主页问题列表**：`QuestionServiceImpl.getQuestionByUserId` 之前没有给 `QuestionDto` 设置作者 `user`，导致头像不显示、链接拼成 `/user/undefined`；现补齐作者（含 power 与脱敏）与 `allCount`。`QuestionList.vue` 同时增加无作者数据的兜底渲染。
- **聊天室打不开**：`sockjs-client` 是 webpack 时代 CommonJS 包，内部直接引用 webpack 注入的 `global`，Vite 下抛 `ReferenceError: global is not defined` 并触发 `[VUE_ROUTER_R0010]`。在 `vite.config.js` 用 `define: { global: 'globalThis' }` + `optimizeDeps.include` 解决；`main.js` 补 `router.onError` 让导航失败有可见提示。实测 `/chat/room` 成功连上 WebSocket 并显示在线人数。
- `PublishView` 编辑态的正文回填改为依赖 Vditor 的 `value` 选项与 `modelValue` 监听，避免 `init()` 被重复调用。

### UI 优化

- **问题详情页**：新增 `MarkdownToc.vue`，用 `Vditor.outlineRender` 复刻原 `#catalogView` 目录，支持点击跳转、滚动高亮与自定义浅色样式（`markdown-toc`）。
- **发布页**：按主流编辑器实践重做 —— 大标题输入 + 分类胶囊 + 独立编辑器卡片 + 右侧粘性「发布设置」（5 项发布前检查、标签 chips 与常用标签折叠面板、验证码、发布按钮）、写作建议卡与广告位；新增正文预览弹窗（`MarkdownView`）。
- **个人设置页**：顶部渐变资料卡（头像/昵称/成就数据）+ 左侧分组导航 + 右侧表单卡；新增 `ImageCropper.vue`（纯 Canvas 实现，无第三方依赖）：拖动定位、滚轮/滑块缩放、三分线、按 1:1 / 3:1 裁剪，输出 JPEG dataURL 后再调用头像/封面上传接口。

### 验证

- `mvn -q -DskipTests compile` 通过；重启后端后用真实管理员会话验证 `/api/admin/topics` 列表（57 条）、编辑保存、标题重复拦截（409）、删除与缓存刷新均正常；`/api/users/{userId}?tab=posts` 的每条问题都带正确作者。
- `npm run build`、oxlint、eslint 全部通过；浏览器实测首页、问题详情目录（49 个目录项）、发布页、设置页裁剪上传、后台话题页（57 张卡片 + 编辑弹窗）、聊天室，控制台均 0 错误；个人主页无 `/user/undefined`，头像正常。

---

## 2026-11-15 · 前后端分离重构（移除 Thymeleaf，后端改为纯 REST API）

本轮把项目从「Spring MVC + Thymeleaf 服务端渲染」重构为「Spring Boot REST API + Vue 3 SPA」。后端只提供 JSON，页面全部由 `web/` 下的 Vue 应用渲染。

### 后端（Java 25 / Spring Boot 4.1.2）

- 移除 `spring-boot-starter-thymeleaf` 依赖与 `src/main/resources/templates/` 下全部 28 个模板；`static/` 仅保留图片、文件等静态资源，继续由后端提供 `/image/**`、`/file/**`。
- 删除 16 个返回视图名的页面控制器（`IndexController`、`QuestionController`、`PublishController`、`SearchController`、`UserController`、`AdminController`、`SignInController` 等），聊天控制器只保留 STOMP `@MessageMapping`。
- 新增 REST 控制器（统一返回 `{ code, message, data }`）：
  - `AuthApiController` — `GET/POST /api/auth/session|login|logout`
  - `MetaApiController` — `GET /api/meta`（热门标签、热门问题、活跃用户、广告位、枚举选项）、`GET /api/meta/ad`
  - `HomeApiController` — `GET /api/home`
  - `QuestionApiController` — `GET /api/questions/{id}`、`GET .../edit`、`POST /api/questions`
  - `DiscoverApiController` — `GET /api/topics`、`/api/topics/{title}`、`/api/search`、`/api/users`、`/api/users/{userId}`
  - `MessageApiController` — `GET /api/messages`、`POST /api/messages/{id}/read`、`POST /api/messages/read-all`
  - `AdminPageApiController` — `GET /api/admin/session|dashboard|users|questions|search|controller|advertisements` 及广告增删改投接口
- 统一鉴权：新增 `AuthUtil`（`requireUser` / `requireAdmin` / `safeUser`），全部基于 Spring Session；`AdminInterceptor` 删除，未登录/非管理员返回 HTTP 401 + JSON 错误码。
- `CustomizeExceptionHandler` 改为 `@RestControllerAdvice`，异常不再跳转错误页；`CustomizeErrorCode` 补充 `NO_ADMIN`、`NOT_FOUND`、`TAG_LIMIT` 等错误码。
- `ResultDTO` 静态方法补全泛型；新增 `CorsConfig`（`allowCredentials=true`，来源由 `app.cors.allowed-origins` 配置）、`WebSocketConfig` 放开 SockJS 来源。
- 点赞/关注接口不再要求前端在 body 里传 JWT：`ClickLikeApiController`、`FollowApiController` 改用会话用户；`FollowTopicService` / `FollowQuestionService` / `FollowUserService` 的 `Claims` 参数替换为 `long userId`，新增 `/api/like`、`/api/follow/{topic,question,user}` 路径（旧路径保留为别名）。
- 新增 `GET /api/captcha`（与 `/captche/images` 共用实现）；`/api/file/upload` 上传成功时返回 Vditor 约定的 `code: 0` 与 `data.errFiles`；修复 `SignUpApiController` 生日为空时的空指针、上传接口空文件空指针。
- `SessionInterceptor` 精简为只做「记住我」token → 会话恢复，不再每次请求查未读消息数。

### 前端（`web/`，Vue 3 + Vite 8）

- 基于已有脚手架补齐依赖：**daisyUI 5 + Tailwind CSS 4**（`@tailwindcss/vite`）、**Vditor ^4.0.0**、**ECharts 6**、`@stomp/stompjs` + `sockjs-client`、Pinia、Vue Router。
- **统一 fetch 组件** `src/api/request.js`：`/api` baseURL、`credentials: 'include'`、JSON/表单/multipart 三种请求体、两类历史响应体归一化、HTTP/业务错误统一 `ApiError`、全局 loading / 错误提示 / 401 回调；`src/api/index.js` 集中管理所有接口 URL。
- 路由 20+ 页面对齐原模板：首页、问题详情、发布/编辑、登录注册、搜索、话题分类、用户列表、个人主页、消息通知、个人设置、关于、聊天规则 + 聊天室、后台登录/仪表盘/用户/问题/搜索/控制面板/广告管理。
- 可复用组件：`AppHeader`、`AppFooter`、`SidePanel`、`AdCarousel`、`Pagination`、`Captcha`、`QuestionList`、`VditorEditor`、`MarkdownView`、`Avatar`、`EChart`、`AdminShell`、`ToastHost`。
- 后台仪表盘使用 ECharts 绘制用户/问题增长曲线；编辑器使用 Vditor 并接入 `/api/file/upload`。
- `vite.config.js` 配置 `/api`、`/file`、`/image`、`/captche`、`/adUrl`、`/ws` 开发代理，保持同源以便 SESSION cookie 自动携带。

### 验证

- `mvn -q -DskipTests compile` 通过；`spring-boot:run` 成功启动（Tomcat 11 / 8080），`/`、`/api/meta`、`/api/home`、`/api/questions/{id}`、`/api/topics`、`/api/users`、`/api/search`、`/api/captcha` 均返回预期结果，未授权接口返回 401 JSON。
- `npm run build` 通过；Vite dev server + 后端代理联调通过。

---

## 2026-11-15 · 后端全量升级：Spring Boot 4.1.2 / Java 25

本轮只升级后端，前端模板与静态资源保持不变。项目由 Spring Boot 2.4.1 / Java 11 升级到 Spring Boot 4.1.2 / Java 25，底层随之切换到 Spring Framework 7、Jakarta EE 11、Tomcat 11、Thymeleaf 3.1。

### 运行时与构建

- `spring-boot-starter-parent` 2.4.1 → **4.1.2**，`java.version` 11 → **25**（本机 JDK 25.0.2 / Maven 3.9.12 验证通过）。
- 依赖升级：`mybatis-spring-boot-starter` 2.1.0 → **4.1.0**（4.x 才是适配 Boot 4 的分支）；`mysql:mysql-connector-java` → `com.mysql:mysql-connector-j` **9.7.0**；`jjwt` 0.9.1 → **0.13.0**；Lombok 1.18.8 → **1.18.48**、commons-lang3 3.9 → **3.20.0**（均改由 parent 统一管理版本）。
- 新增 `software.amazon.awssdk:s3` 与 `url-connection-client` **2.59.1**；显式引入 `com.fasterxml.jackson.core:jackson-databind` 供 jjwt-jackson 使用。
- 移除：`spring-boot-starter-hateoas`、`spring-boot-starter-reactor-netty`（项目未使用）、`javax.xml.bind:jaxb-api`、`maven-javadoc-plugin`、spring-boot-maven-plugin 的 `includeSystemScope`。
- **构建适配**：JDK 23+ 起 javac 不再自动扫描 classpath 发现注解处理器，需在 `maven-compiler-plugin` 中通过 `annotationProcessorPaths` 显式声明 Lombok，否则所有 Lombok 生成的 getter/setter 都会编译报「找不到符号」。

### Jakarta 命名空间迁移

- 32 个 Java 文件的 `javax.servlet.*` → `jakarta.servlet.*`（共 42 处导入）。
- 清理 `FollowQuestionMapper` 中未使用的 `javax.annotation.Priority` 与 `PostMapping` 导入。

### 数据库连接池

- 移除 `com.alibaba:druid` 及 `spring.datasource.type: com.alibaba.druid.pool.DruidDataSource` 配置，改由 Spring Boot 4 默认的 **HikariCP** 接管，并补充连接池参数。
- 测试环境实测 `192.168.0.100:3306` 使用 `root/123456`、库 `community`（13 张表齐备）；配置文件同步更新。

### 验证码

- 删除 `libs/EasyCaptcha-1.5.0-RELEASE.jar` 及 pom 中的 `system` scope 依赖。
- 改为项目内自研模块 `captcha/ImageCaptcha.java` + `captcha/CaptchaUtil.java`：基于 `SecureRandom` 与 Java2D 生成 130×48 PNG，验证码文本以小写形式存入 HttpSession、忽略大小写校验、5 分钟过期；随机数不再使用不安全的 `Random`（规避 CVE-2018-18531）。
- 保持原有 `out()/ver()/clear()` 调用契约，8 处调用点仅修改 import，`/captche/images` 端点与全部前端页面无需改动。
- 选择自研而非引入主流 Maven 验证码库，是因为 easy-captcha 仍编译在 `javax.servlet` 上，与 Jakarta EE 11 不兼容。

### 对象存储

- `io.minio:minio` 3.0.10 → **AWS SDK v2 S3** 实现。
- `MinIOConfigProperties` / `MinIOStorageRepositoryImpl` 替换为 `S3ConfigProperties` / `S3StorageRepositoryImpl`；`MyConfig` 提供 `S3Client`（上传）与 `S3Presigner`（预签名 URL）两个 Bean，显式使用 JDK 自带的 `UrlConnectionHttpClient`，开启 path-style 访问以兼容 MinIO 等 S3 兼容服务。
- `FileStorageRepository` 接口契约不变；对象名自动去除前导 `/`，预签名有效期上限收敛为 SigV4 的 7 天。
- 配置项由 `minio.*` 改为 `s3.*`（endpoint / region / access-key / secret-key / bucket / out-time / path-style-access）。

### 安全与会话

- `JwtUtil` 迁移到 jjwt 0.13.0 新 API（builder / parser 全部改写）；HS256 现在强制要求 32 字节密钥，因此对原配置项 `jwt.key` 做 SHA-256 派生，无需修改配置值。
- `FileApiController` 中唯一的 fastjson `JSONObject` 改为 `Map`，移除 `com.alibaba:fastjson` 依赖。
- Spring Boot 4 默认使用 Jackson 3 (`tools.jackson.*`)，Spring Session 的序列化器由已标记为待删除的 `Jackson2JsonRedisSerializer` 改为 `GenericJacksonJsonRedisSerializer`（Jackson 3，开启默认类型信息以保证会话内自定义对象仍能反序列化为原类型）。
- 会话仍由 Spring Session + Redis 承载；实测测试环境 Redis 未设置密码，已移除配置中错误的 `pzw123456`。

### 配置项迁移

- `spring.redis.*` → `spring.data.redis.*`。
- `logging.file` / `logging.file.max-history` / `logging.file.max-size` → `logging.file.name` + `logging.logback.rollingpolicy.*`。
- 删除 Boot 4 中已不存在的 `spring.mvc.favicon.enabled`。
- 新增 `spring.application.name` 与 `s3.*` 配置段。

### 验证结果

- `mvn clean package -DskipTests` **BUILD SUCCESS**，产出 `target/community-0.0.1-SNAPSHOT.jar`。
- JDK 25 实际启动成功：Spring Boot 4.1.2 / Tomcat 11.0.27，约 8.8 秒；HikariCP 连库成功，热门话题、热门问题、活跃用户、管理数据等定时任务查询均正常，启动日志无 ERROR。
- 页面冒烟：首页、`/sign-in`、`/sign-up`、`/class`、`/about`、`/question/1`、`/admin`、`/chat` 均返回 200，Thymeleaf 3.1 未发现模板解析异常。
- 功能验证：验证码接口返回合法 PNG；验证码会话校验正确返回「验证码错误！」；`/api/checkEmail` 返回正常 JSON；S3 预签名 URL 生成正确；JJWT 签发与解析往返成功。

### 已知事项 / 待办

- `s3.endpoint` 暂沿用旧 MinIO 地址 `http://192.168.147.132:9000`，当前不可达，需改为实际可用的 S3 / MinIO 端点后再验证 `putObject` 上传。
- 前端（Thymeleaf 模板、Vditor、Bootstrap4 等）本轮未改动，留待后续单独升级。

---

## 历史更新（原 README.md「更新日志」章节）

以下条目完整迁移自 `README.md`，保留原始描述。

### 2020-03-29

使用Spring Session Redis

### 2020-03-08

添加对象存储MinIO，升级Spring版本

### 2019-11-10

增加一个在线聊天室

### 2019-11-07

增加远程查看日志功能，在管理员界面查看

### 2019-11-04

添加广告投放功能，进入admin页面即可进行投放广告

### 2019-11-01

优化消息通知显示，可以显示对方回复内容

### 2019-10-27

完成关注用户，话题以及问题

### 2019-10-26

优化发帖时间显示

### 2019-10-24

添加多级评论，点击评论可以 @ 用户

### 2019-10-20

替换 markdown 编辑器为更加好用的 Vditor

修改数据库字符集为 utf8mb4 以便用于更好的支持 emoji

### 2019-10-15

完成话题分类页

### 2019-09-27

完成评论点赞，删除评论

### 2019-09-25

添加问题点赞功能，待完成评论点赞

暂时移除 spring-session-jdbc 等待修复添加 spring-session-jdbc 后造成的登陆序列化异常 BUG

### 2019-09-22

添加 spring-session-jdbc，修改配置文件为 yml，删除不必要的包
