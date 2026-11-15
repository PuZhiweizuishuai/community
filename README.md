# 不挂高数社区

一个面向开发者的问答社区：提问、回答、评论、点赞、关注话题与用户、消息通知、Markdown 编辑（含正文目录）、文件上传与裁剪、白天/夜间主题、在线聊天室（单窗口），以及后台管理（用户/问题/动态分类/话题/广告/日志）。

- 在线访问：<https://community.buguagaoshu.com/>
- 参考项目：[mawen 社区](http://www.mawen.co)、[codedrinker/community](https://github.com/codedrinker/community)
- 源码：<https://github.com/PuZhiweizuishuai/community>

## 架构

**前后端分离**：`src/` 是纯 REST API 后端，`web/` 是 Vue 3 单页前端，两者独立启动、独立部署。

| 层 | 实现 |
| --- | --- |
| 后端 | Java 25、Spring Boot 4.1.2、Spring MVC（无 Thymeleaf）、Spring WebSocket/STOMP |
| 持久层 | MyBatis 注解 Mapper、MySQL Connector/J、HikariCP、Spring Session + Redis |
| 其他 | AWS SDK v2 S3（兼容 MinIO）、JJWT、BCrypt、自研图形验证码 |
| 前端 | Vue 3、Vite 8、Vue Router、Pinia、daisyUI 5 / Tailwind CSS 4、Vditor 4、ECharts 6 |
| 通信 | 统一 fetch 组件 → `/api/**`；登录态用 SESSION cookie；聊天用 SockJS + STOMP `/ws` |

> 完整更新历史见 [CHANGELOG.md](CHANGELOG.md)，前端运行与接口说明见 [web/README.md](web/README.md)。
> 已有数据库请执行 [SQL_Upgrade/001_performance_and_consistency.sql](SQL_Upgrade/001_performance_and_consistency.sql)（幂等，含索引与字符集优化），回滚见同目录 `001_rollback.sql`。

## 快速开始

环境：Java 25、Maven 3.9+、Node 22.18+/24.12+、MySQL 8+（`utf8mb4`）、Redis、S3 兼容对象存储。先用 `databases.sql` 建库并配置 `src/main/resources/application.yml`。

```bash
# 1) 启动后端（http://localhost:8080，只提供 JSON API）
mvn spring-boot:run

# 2) 启动前端（http://localhost:5173）
cd web
npm install
npm run dev
```

开发时 Vite 已把 `/api`、`/file`、`/image`、`/captche`、`/adUrl`、`/ws` 代理到后端；生产构建用 `npm run build`，把 `web/dist` 部署到静态服务器，并设置 `VITE_API_BASE` / `VITE_ASSET_BASE` 指向后端地址（同时把前端域名加入 `app.cors.allowed-origins`）。

打包后端：`mvn clean package` → `java -jar target/community-0.0.1-SNAPSHOT.jar`。

> 第一个管理员账号：注册后手动把数据库权限表 `power` 改为 `0`，再从 `/admin/login` 进入后台。

## 目录速览

```
src/main/java/com/buguagaoshu/community/
  controller/api/   所有 REST 接口（页面数据 + 业务动作）
  config/           跨域、会话序列化、S3、WebSocket
  interceptor/      记住我 token → 会话恢复
  exception/        全局 JSON 异常处理
  dto/model/mapper/service/cache/schedule/util
web/src/
  api/              统一 fetch 组件 + 接口清单
  router/ stores/   路由与 Pinia 状态
  components/       通用 UI 组件
  views/            前台页面 + views/admin 后台页面
```

## 资料

[Spring Boot 文档](https://docs.spring.io/spring-boot/reference/) · [Spring Session](https://docs.spring.io/spring-session/reference/) · [MyBatis](http://www.mybatis.org/mybatis-3/zh/index.html) · [Vditor](https://hacpai.com/article/1549638745630) · [daisyUI](https://daisyui.com/) · [ECharts](https://echarts.apache.org/) · [AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
