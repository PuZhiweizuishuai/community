# web · 不挂高数社区前端

Vue 3 + Vite 8 单页应用，配套 `src/` 下的 Spring Boot REST API。UI 使用 daisyUI 5 / Tailwind CSS 4，Markdown 编辑与渲染用 Vditor 4，后台图表用 ECharts 6。

## 运行

```bash
npm install
npm run dev          # http://localhost:5173
npm run build        # 产物 web/dist
npm run preview      # 预览生产构建
```

后端需同时运行在 `http://localhost:8080`（`mvn spring-boot:run`）。`vite.config.js` 已把 `/api`、`/file`、`/image`、`/captche`、`/adUrl`、`/ws` 代理到后端，因此开发阶段前后端同源，Spring Session 的 `SESSION` cookie 可自动携带。

自定义后端地址：

```bash
VITE_BACKEND_URL=http://192.168.0.100:8080 npm run dev
```

生产独立部署时创建 `.env.production`（参考下面两项），并同步在后端 `application.yml` 的 `app.cors.allowed-origins` 中加入前端域名。

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `VITE_API_BASE` | `/api` | 接口 baseURL，例如 `https://api.example.com/api` |
| `VITE_ASSET_BASE` | `''` | 后端静态资源前缀，例如 `https://api.example.com` |
| `VITE_BACKEND_URL` | `http://localhost:8080` | 仅开发代理使用 |

## 统一请求组件

所有请求都经过 `src/api/request.js`，视图层只调用 `src/api/index.js` 中的方法：

```js
import { api } from '@/api'
import { homeApi, questionApi } from '@/api'

const home = await homeApi.list({ page: 1, sort: '3' })   // GET /api/home
await questionApi.save({ title, description, ... })        // POST /api/questions（JSON）
await api.form('/admin/login', { email, password })        // 表单编码（兼容老接口）
const raw = await api.upload('/file/upload', formData)     // multipart，保留原始响应
```

组件能力：

- `credentials: 'include'`，统一超时（20s）、`AbortController`、全局 loading 计数。
- 自动归一化统一响应体 `{ status, message, timestamp, data }`：成功只返回 `data`，失败统一抛 `ApiError`（含 `status` / `payload`），HTTP 状态码与响应体 `status` 一致。
- 全局回调：`setErrorHandler`（toast）、`setUnauthorizedHandler`（401 跳登录/后台登录），在 `src/main.js` 注册。
- `assetUrl()` 把后端返回的 `/image/...`、`/file/...` 转成可访问地址；`captchaUrl()` 生成带随机参数的验证码图片地址。

## 目录

```
src/api/          request.js（统一 fetch）、index.js（接口清单）
src/router/       路由 + 登录/管理员守卫
src/stores/       session（登录态）、meta（侧栏公共数据）、toast
src/components/   AppHeader / AppFooter / SidePanel / AdCarousel / Pagination /
                  Captcha / QuestionList / VditorEditor / MarkdownView /
                  MarkdownToc（正文目录）/ ImageCropper（Canvas 裁剪）/
                  SiteLogo（压缩后的品牌 Logo）/ ThemeToggle（主题切换）/
                  Avatar / EChart / AdminShell / ToastHost
src/stores/theme.js   system / light / dark 三种模式，localStorage 持久化
src/utils/searchHistory.js  搜索历史（localStorage，最多 10 条）
src/components/AuthShell.vue  登录/注册共用的品牌分栏布局
src/views/        前台页面；views/admin 为后台页面
src/assets/       main.css（Tailwind + daisyUI + Markdown 排版）
```

## 与后端约定的要点

- 登录态：`GET /api/auth/session` 返回 `{ user, admin, permissions, banned, ban, notificationCount, loggedIn, isAdmin }`；`admin` 与 `permissions` 由后端 RBAC 角色权限计算，登录/退出为 `POST /api/auth/login|logout`。
- 需要登录的接口返回 HTTP 401 + `{ status: 401, message }`；需要后台权限的接口按权限点校验，未授予 `admin:access` 或缺少具体权限统一返回 HTTP 403 + `{ status: 403, message }`。
- 社区写接口按权限点校验：发布/编辑问题 `content:question:publish`、发表评论 `content:comment:create`、上传文件 `file:upload`；被封禁用户的写请求统一返回 HTTP 403 与封禁提示。
- 分页对象字段：`data`、`nowPage`、`totalPage`、`pages`、`showFirstPage`、`showPrevious`、`showNext`、`showEndPage`、`allCount`。
- Vditor 上传字段名 `file[]`，为兼容编辑器协议，`/api/file/upload` 仍返回 `{ code: 0, msg, data: { errFiles, succMap } }`，是统一响应体之外的唯一特例。
- 聊天室：SockJS 连接 `/ws`，订阅 `/topic/public`，发送 `/app/chat.addUser` 与 `/app/chat.sendMessage`。`sockjs-client` 依赖 webpack 注入的 `global`，因此 `vite.config.js` 里用 `define: { global: 'globalThis' }` 做兼容，删除该配置会导致聊天室白屏。
- 后台话题分类：`/admin/topics` 对应 `GET/POST /api/admin/topics*`，保存后服务端立即刷新 `TagClassCache`；话题图标可复用 `/api/file/image/upload`（登录用户含管理员均可上传）。
- 图片裁剪：`ImageCropper.vue` 输出 JPEG dataURL，头像走 `/api/update/HeadUrl`（imgBase），封面转成 Blob 后走 `/api/file/image/upload`（字段名 `editormd-image-file`，`type=top`）。
- 主题：`<html data-theme>` 由 daisyUI 5 驱动；`index.html` 的内联脚本先读 `localStorage('community-theme-mode')` 再决定首屏主题。自定义组件颜色走 `main.css` 里的 CSS 变量，`[data-theme='dark']` 下还有针对显式 slate / white 工具类的覆盖。
- **Vditor 主题**：代码高亮/内容主题由 Vditor 运行时从 CDN 注入 `<link>`，只传 `preview.mode='dark'` 不会切换。统一走 `utils/vditorTheme.js` 的 `vditorPreviewTheme(isDark)`；`main.css` 里另有 `[data-theme='dark']` 作用域的 GitHub Dark token 配色兜底，CDN 失败也不会变回白底代码块。
- **代码块底色**：由 `--code-block-bg` / `--code-block-color` 控制，必须与 Vditor 当前高亮主题一致（白天 `#fff`、夜间 `#0b1120`）；`pre > code.hljs` 强制 `background: transparent; padding: 0`，底色与内边距只由 `pre` 提供，避免白天出现「深色外框 + 白色内块」。
- 动态分类：`/api/meta` 的 `classifications`（问题分类，value=0 为全部）与 `topicTypes`（话题所属分类）来自后端的 `classification` 表；后台 `/admin/classifications` 维护，`/admin/topics` 按分类分组展示。删除分类时后端会校验关联问题数并拒绝。
- 聊天室单窗口：前端每个窗口生成 `clientToken`，JOIN / CHAT 都带上；服务端 `ChatPresenceRegistry` 按 userId 占用席位，重复窗口收到 `type: DUPLICATE` 消息并自行断开。气泡使用互斥类名 `chat-bubble-mine / chat-bubble-other`，不要再叠加 `bg-white` 与 `bg-indigo-600`。
- 搜索历史：`localStorage('community-search-history')`，导航搜索框下拉与搜索页 chips 共用 `utils/searchHistory.js`。
- 广告批量投放：`/api/admin/advertisement/positions` 查容量，`/batch-setting`、`/batch-close` 返回逐条回执；前端列表支持多选与批量操作。
- 后端缓存：`SiteCacheService` 提供统一 TTL 缓存，`/api/admin/cache` 查看命中率、`/refresh` 清空；后台「控制面板 → 缓存与统计」可直接操作。
