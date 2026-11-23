# 项目长期笔记（Community）

## Git 远端约定

- **2026-11-15 起：默认远端推送只到 GitHub**（`https://github.com/PuZhiweizuishuai/community.git`），不再推送 Gitee。`origin` 只保留一个 URL（fetch 与 push 相同），没有单独的 `pushurl`。
- 若以后确需推送 Gitee，手动指定：`git push https://gitee.com/puzhiweizuishuai/community.git master`。
- 历史背景：GitHub 与 Gitee 曾长期分叉，2026-11-15 用两次 `git merge -s ours` 把两边提交历史合并进本地（代码内容以本地升级为准），之后两边都能快进推送。本地仍保留 `refs/remotes/gitee/master` 跟踪引用，不影响推送。
- 本环境 git 访问 GitHub 可能报 `CRYPT_E_REVOCATION_OFFLINE`；用 `git -c http.sslBackend=openssl ...` 可正常联网且保留证书校验，不要长期关闭 `http.sslVerify`。

## 技术栈基线（2026-11-15 后端升级后）

- Spring Boot 4.1.2 / Java 25 / Jakarta EE 11 / Tomcat 11；**2026-11-15 已改为前后端分离，Thymeleaf 依赖与 templates 全部移除**，后端只提供 `/api/**` JSON，页面在 `web/`（Vue 3 + Vite 8）。
- 前端：daisyUI 5 + Tailwind CSS 4（`@tailwindcss/vite`）、Vditor ^4、ECharts 6、Pinia、Vue Router、`@stomp/stompjs` + `sockjs-client`；统一 fetch 在 `web/src/api/request.js`。
- MyBatis starter 4.1.0 + MySQL Connector/J 9.7.0 + HikariCP；Spring Session + Redis（Jackson 3）。
- AWS SDK v2 S3 2.59.1（兼容 MinIO）、JJWT 0.13.0、项目内自研图形验证码；fastjson / Druid / MinIO SDK / EasyCaptcha 均已移除。
- JDK 23+ 必须在 `maven-compiler-plugin` 配置 `annotationProcessorPaths` 指向 Lombok，否则编译失败。
- **分类已动态化**：`classification` 表（`kind=question|topic`）由 `ClassificationSchemaInitializer` 自动建表 + 枚举播种，`/api/meta` 与 `/api/admin/classifications` 提供读写；不要再新增写死的分类枚举。删除分类前必须校验关联问题数。
- **主题**：`web/src/stores/theme.js` + `<html data-theme>` + localStorage `community-theme-mode`；新增组件请优先用 `main.css` 的 CSS 变量，避免硬编码 `#fff` / slate 色值。
- **聊天室单窗口**：`ChatPresenceRegistry` 按 userId 占用席位，前端 JOIN/CHAT 必须带 `clientToken`；改动聊天逻辑时保持该约束。聊天气泡必须用互斥类名，避免白底白字。
- **缓存**：`SiteCacheService` 是全站统一 TTL 缓存（热门标签/问题、动态分类、话题、首页列表、在线人数）。新增读多写少接口时优先 `get(key, ttl, loader)`，并在对应写操作里 `invalidate(prefix)`；`/api/admin/cache` 可看命中率。
- **数据库变更约定**：`databases.sql` 面向全新安装；已有库的升级一律写幂等脚本放到 `SQL_Upgrade/`，并配套 rollback 脚本。MySQL 无 `CREATE INDEX IF NOT EXISTS`，用 `information_schema.statistics` + 存储过程判断。本机无 mysql CLI，改用 MySQL Connector/J + 自写 JDBC runner 验证。
- **前端样式约定**：条件颜色用互斥类名（不要默认类 + 覆盖类叠加）；写死颜色前先看 `main.css` 是否有对应 CSS 变量，保证夜间模式可用。
- **2026-11-16 起统一设计系统**：颜色 / 文字层级一律用 `main.css` 的 CSS 变量与语义类 `ui-text-strong|base|muted|brand`、`ui-bg|ui-bg-soft|ui-bg-brand|ui-bg-page`、`ui-border`，**不要再写 `text-slate-*` / `bg-white` / `border-slate-*`**；圆角用 `--radius-*`、阴影用 `--shadow-*`；卡片标题用 `.section-title`、空状态用 `.empty-state`、标签用 `.chip`。新组件沿用这套类，暗色模式自动适配。
- **移动端布局**：`<lg` 时 AppHeader 渲染固定底部导航（因此 `App.vue` 的 main 需保留 `pb-24 lg:pb-6`）；后台侧栏 `<md` 为抽屉；全屏页用 `100dvh` + `env(safe-area-inset-*)`；输入框在 `<sm` 由 CSS 强制 16px 防 iOS 缩放。
- **Vditor 主题**：代码高亮与内容主题是运行时 CDN 注入的 `<link>`，`preview.mode` 不管这个；统一走 `web/src/utils/vditorTheme.js` 的 `vditorPreviewTheme(isDark)`，编辑器切换主题要调 `setTheme(editor, content, code)` 三参数。`main.css` 有 `[data-theme='dark']` 作用域的 GitHub Dark 兜底，改动 Vditor 相关组件时别删。
- **后台列表页统一范式**（评论管理 → 问题管理已照此重构）：页头 `surface` 卡片（渐变图标 + 标题 + 说明 + 刷新）+ 筛选 `surface`（`ui-bg-soft` 胶囊里放状态标签、`共 N 条`、右侧 join 搜索）+ `surface overflow-hidden` 表格（空状态用 `Inbox`，加载/空行用 `colspan`）+ `Pagination` + 详情 `dialog.modal modal-open` 弹窗，条件全部放路由 query（`status`/`keyword`/`page`）。激活标签用 `ui-bg`（不要 `bg-white`，夜间是白块）；表格中文标题列必须给 `min-w-[260px]` 否则被挤成极窄多行。

## 聊天室（2026-11-23 改版）

- 输入区是 `components/ChatComposer.vue`：Vditor `ir` 模式，**只做 Markdown 编辑，不接上传**（`upload.handler` 返回提示，零后端请求）。Enter 发送、Shift+Enter 换行（用 `execCommand('insertParagraph')` 补，Vditor 本身不支持软换行）、Ctrl/⌘+Enter 发送；`caretInStructure()` 判断光标在 li / blockquote / pre / table 或正在输入块级语法时让编辑器接管回车。
- 注意 3 个 Vditor 容器坑：① **IR 编辑区根节点本身就是 `<pre class="vditor-reset">`**，`closest('pre')` 判断"光标在代码块里"会永远命中，导致回车永不发送，判断时要排除该根节点；② 外层卡片不能用 `overflow:hidden`，会裁掉 Vditor 向上弹出的工具栏 tooltip / 表情面板，同时工具栏要 `z-index:5` 盖住输入区；③ Vditor 自带 `.vditor-content{min-height:60px}`，固定高度容器必须覆盖成 `min-height:0`，否则内容溢出盖住下方按钮。
- **中文输入法回车**：组合态 Enter 的 keydown 是 `isComposing=true`/keyCode 229，不要在 keydown 直接发送（第一下会被吞，用户要按两次）。做法：keydown 只 `preventDefault+stopPropagation` 拦 Vditor，发送放 keyup + `compositionend.capture` 兜底并用 rAF 等 input 回调同步；发送条件也不要依赖 Vditor 的 `after`（等 CDN Lute 加载完才 ready）。
- 消息渲染走 `components/ChatMarkdown.vue`（`Vditor.md2html` + LRU 缓存），Lute 从 unpkg 加载约 3.7MB，加载期间务必先显示纯文本，否则气泡是空白。
- `ChatMessage` 带 `time`（毫秒）与 `members`（仅 JOIN/LEAVE 下发），前端据此做日期分隔、合并与在线成员列表；`sendMessage` 服务端限制 4000 字符。

## 安全基线（2026-11-22 起）

- **安全响应头**：`config/SecurityHeadersFilter` 统一注入 X-Frame-Options / nosniff / Referrer-Policy / Permissions-Policy / COOP / CSP，HTTPS 时加 HSTS（按 `request.isSecure()` 或 `X-Forwarded-Proto`）。`/api/file/**` 不设 CSP。
- **CSP 与 Vditor**：`script-src 'self'`（不含 unsafe-inline）。Vditor 会注入内联 `onclick` 的代码块复制按钮，必须靠 `web/src/utils/vditorCsp.js` 剥离 + 事件委托；新增 Vditor 使用点时记得接入。HTML 里不要写内联 `<script>`（首屏主题脚本在 `web/public/theme-boot.js`）。CSP 出问题先看 `app.security.csp-enabled`。
- **Cookie**：手工 `@EnableRedisHttpSession` 后 `server.servlet.session.cookie.*` 不生效，SESSION cookie 的 Secure/HttpOnly/SameSite 由 `HttpSessionConfig.cookieSerializer` 统一控制（读 `COOKIE_SECURE`）；「记住我」token cookie 走 `app.cookie.secure`。生产用 `SPRING_PROFILES_ACTIVE=prod`（关 Swagger、Secure 默认 true、导入 application-local.yml 凭据）。
- **鉴权**：`PermissionInterceptor` 对 `/api/admin/**` 默认拒绝（白名单只有 login/logout/session）。新增后台接口务必标 `@RequirePermission`；用户侧操作不要放 `/api/admin/` 下（作者删帖是 `DELETE /api/questions/{id}`，删评论是 `DELETE /api/comment/{id}`）。
- **登录**：邮箱 + IP 双维度限流（`RateLimitService.isLocked(key, max)`），失败统一 `SIGN_IN_CREDENTIALS_ERROR`，日志用 `StringUtil.maskEmail`。密码规则 8~30 位且含字母+数字，前端校验统一 `web/src/utils/validate.js`。
- **上传**：`FileUtil.detectMagicType` / `isMagicConsistent` 对所有允许类型做文件头一致性校验（OOXML/APK 按 ZIP、老 Office 按 OLE2）。
- **HTTPS 不在应用层做**：由 Nginx 终止 TLS；应用需 `server.forward-headers-strategy: native`，Nginx 必须传 `X-Forwarded-Proto` / `X-Forwarded-For`。


## 测试环境

- MySQL `192.168.0.100:3306`，账号 `root` / `123456`，库 `community`（原 13 张表 + 新增 `classification`，由启动器自动建表）；Redis 同机 6379，**无密码**。
- 浏览器自动化：`playwright-cli` 本机无 Chrome，须用 `--browser=msedge`；会话只在同一条 Bash 链内存活。Spring Session 的 `SESSION` cookie 是 sessionId 的 **base64**，可用 `curl -b "SESSION=<base64>"` 复用已有管理员会话验证接口。
- 旧 MinIO 端点 `192.168.147.132:9000` 不可达，`s3.endpoint` 需改成实际可用地址。
- 更新记录统一写 `CHANGELOG.md`；`README.md` 只保留简介与指向链接。
