# 不挂高数社区（名字可以随便改的）

## 参考项目

[mawen 社区](http://www.mawen.co)

[community](https://github.com/codedrinker/community)

## 介绍

目前还是MVC模式，开发完成后可能会考虑前后端分离


已完成登陆注册,发帖，回复评论，markdown编辑，消息通知，图片上传,个人界信息编辑，热门话题，问题与用户的搜索，以及后台管理（在主页输入url /admin 进入）设置首页置顶问题

部分权限验证

待完成点赞，热门帖子，文件上传，找回密码，删除未使用图片

后端使用 Spring boot 和  mybatis 开发

密码加密采用 Spring Security 的 Bcrypt 加密

页面模板使用 Thymeleaf

前端页面设计使用 BootStrap4 

热门话题每三小时更新一次

每两小时处理一次离线用户

每六小时同步一次管理数据


## 快速运行
环境准备

Java版本： Java 8 及以上

maven: 3

数据库： MySQL 或者 MariaDB 或者 H2 （使用H2数据库需要在pom文件中添加H5依赖）

```xml
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>1.4.199</version>
        </dependency>
```

使用 database.sql 脚本创建数据库
进入 REALEAS 文件下配置 application.properties 

运行打包命令
```bash
mvn clena package
```
运行项目
```bash
java -jar community-0.0.1-SNAPSHOT.jar
```
即可

**注意：第一个管理员账号需要在创建完数据库运行项目注册第一个账户后，手动将数据库的权限表power列数据该为0**

**第一次运行如果没有修改配置文件中上传图片保存路径与日志路径的化，系统会默认在程序运行的目录下创建file文件夹和log文件夹，分别保存图片文件和日志文件！**

也可以在 IDEA 中导入此项目，如果 getter 和 setter 飘红，则需要安装 IDEA 得到 Lombok 插件

## 页面展示

(ps：如果图片不显示，请到images文件夹下查看)

主页

![主页](https://github.com/PuZhiweizuishuai/community/blob/master/images/home.jpg "主页")

用户首页

![用户首页](https://github.com/PuZhiweizuishuai/community/blob/master/images/userHome.jpg "用户首页")

用户信息修改页

![用户信息修改页](https://github.com/PuZhiweizuishuai/community/blob/master/images/userchange.jpg "用户信息修改页")


问题展示页

![](https://github.com/PuZhiweizuishuai/community/blob/master/images/questionPage.jpg)

![](https://github.com/PuZhiweizuishuai/community/blob/master/images/questionPage2.jpg)

![](https://github.com/PuZhiweizuishuai/community/blob/master/images/questionPage3.jpg)

消息提醒页

![](https://github.com/PuZhiweizuishuai/community/blob/master/images/message.jpg)

管理员页面

![](https://github.com/PuZhiweizuishuai/community/blob/master/images/adminPage.jpg)

![](https://github.com/PuZhiweizuishuai/community/blob/master/images/adminPage%20(2).jpg)


## application.properties 配置文件介绍

**# 数据库驱动**

spring.datasource.driver-class-name=com.mysql.jdbc.Driver

**数据库连接地址**

spring.datasource.url=jdbc:mysql://192.168.147.129:3306/community?useUnicode=true&characterEncoding=UTF-8

**# 数据库账号**

spring.datasource.username=
 
**# 数据库密码**
spring.datasource.password=

**# 运行端口号**

server.port=8080

**# 阿里巴巴数据库连接池简单配置**

spring.datasource.type=com.alibaba.druid.pool.DruidDataSource

**# 文件配置**

**# 默认将在软件运行目录下创建一个名叫file的文件夹用于保存文件**

File.ROOT.PATH=file

**# 限制上传单文件最大大小**

spring.servlet.multipart.max-file-size=10MB

**# 限制所有文件最大大小**

spring.servlet.multipart.max-request-size=1024MB

**# 关闭spring boot 自己的 favicon**

spring.mvc.favicon.enabled=false

**# 日志文件目录**

logging.file=log/spring.log

**# 正常的日志级别**

logging.level.root=info

**# 显示 mapper 目录下日志级别**

**# logging.level.com.buguagaoshu.community.mapper=debug**

**# 单日志文件大小**

logging.file.max-size=1MB

**# 要保留的最大存档日志天数**

logging.file.max-history=7

index.page.size=10

**# jwt 密钥**

jwt.key=buguagaoshu

**# 默认token有效期两小时**

jwt.ttl=7200000


## 资料
[spring 入门指南](https://spring.io/guides)

[spring boot 日志](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-logging.html)

[spring thymeleafk快速指南](https://spring.io/guides/gs/serving-web-content/)

[mybatis 文档](http://www.mybatis.org/mybatis-3/zh/index.html)

[mybatis-spring-boot-autoconfigure](http://www.mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)

[Java web开发基础](http://jinxuliang.com/course/CoursePortal/Details/5a9268a9a664d72f041e0a6a)

[BootStrap4 中文文档](http://bs4.ntp.org.cn/)

## 工具
在线 markdown 编辑器 [markdown edit](https://pandao.github.io/editor.md/)

JavaScript剪裁图片工具 [cropperjs](https://github.com/fengyuanchen/cropperjs)

JavaScript定位引擎 [popper.js](https://github.com/FezVrasta/popper.js)

BootStrap 日历选择插件 [Tempus Dominus](https://tempusdominus.github.io/bootstrap-4/)

节省多余的Java代码 [Lombok](https://www.projectlombok.org)

后端生成验证码的工具包 [EasyCaptcha 验证码](https://github.com/whvcse/EasyCaptcha)
注意：此工具和kaptcha有一样的问题，使用的Random方法有不安全，[CVE-2018-18531](http://www.cnnvd.org.cn/web/xxk/ldxqById.tag?CNNVD=CNNVD-201810-1111)对安全性要求较高的，建议下载源码后将Random方法更改为SecureRandom

更新：最新版修复了这个问题

JWT验证工具 [JJWT Java JWT](https://github.com/jwtk/jjwt)

## 代码文件结构

cache 缓存

component 组件

config 自定义配置

controller 控制层

dto DAO层负责页面与后端程序的数据传输

enums 各种数据的类型

exception 异常处理

interceptor 过滤器

mapper 数据库映射

model 定义数据库和后端程序的数据交换类型

schedule 定时任务

service 服务层

util 各种工具


