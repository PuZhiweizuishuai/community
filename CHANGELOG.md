# 更新日志（Changelog）

本文件记录「不挂高数社区」的更新历史。原 `README.md` 中的「更新日志」章节已整体迁移至此，并补充 2026-11-15 的后端全量升级记录。

按时间倒序排列。

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
