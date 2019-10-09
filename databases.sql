-- 创建数据库
create database community;
use community;

--查看数据库编码
show variables LIKE 'collation_%';

--如果不是utf8 collation_database 则修改为 utf8 编码
--修改数据库编码属性
ALTER DATABASE community CHARACTER SET utf8 COLLATE utf8_unicode_ci;


-- TODO 修改时间为bigint 方便后期排序

--创建用户表
CREATE TABLE users
(
    id bigint,
    userID VARCHAR(20) NOT NULL,
    userName VARCHAR(20) NOT NULL,
    password VARCHAR(60) NOT NULL,
    email VARCHAR(30) NOT NULL,
    sex VARCHAR(2) NOT NULL,
    age int,
    birthday varchar(19),
    school varchar(20),
    major varchar (20),
    selfIntroduction varchar (500),
    simpleSelfIntroduction varchar (50),
    likes varchar (50),
    likeCount bigint default 0,
    creationTime VARCHAR(19),
    lastTime VARCHAR(19),
    headUrl VARCHAR(300),
    userTopPhotoUrl varchar (300),
    PRIMARY KEY (id)
);
--
ALTER TABLE community.users ADD UNIQUE (userId);
--设置邮箱不重复
ALTER TABLE community.users ADD UNIQUE (email);

--设置主键自增
alter table users modify id bigint auto_increment;
--设置主键从1000开始自增
alter table users AUTO_INCREMENT=1000;


--用户权限表
create table userPermission
(
    id bigint,
    power int default 1,
    modifier varchar(20) not null,
    updateTime varchar(19) not null,
    dueTime BIGINT,
    PRIMARY KEY (id),
    constraint userPermission_FK foreign key(id) references users(id)
);

--在线用户表
create table onlineUser
(
    id bigint,
    userName VARCHAR(20) NOT NULL,
    token varchar (300) not null ,
    ip varchar (128) not null ,
    time bigint not null ,
    expireTime bigint default 0,
    PRIMARY KEY (id),
    constraint onlineUser_FK foreign key(id) references users(id)
);

--问题表
CREATE TABLE Questions
(
    questionId bigint PRIMARY KEY AUTO_INCREMENT,
    userId bigint NOT NULL,
    title varchar(50),
    classification varchar (50),
    description text,
    fileUrl text,
    viewCount bigint DEFAULT 1,
    commentCount bigint DEFAULT 0,
    likeCount bigint DEFAULT 0,
    followCount bigint default 0,
    tag varchar(256),
    createTime bigint,
    alterTime bigint,
    status int default 1,
    constraint user_question_FK foreign key(userId) references users(id)
);

CREATE INDEX index_question_createTime ON Questions (createTime);
CREATE INDEX index_question_alterTime ON Questions (alterTime);

-- 评论表
CREATE TABLE comment
(
    commentId bigint PRIMARY KEY AUTO_INCREMENT,
    questionId bigint not null,
    parentId bigint NOT NULL,
    type int NOT NULL,
    commentator bigint NOT NULL,
    content text NOT NULL,
    likeCount bigint DEFAULT 0,
    commentCount bigint default 0,
    createTime bigint NOT NULL,
    modifiedTime bigint NOT NULL,
    status int default 1,
    constraint comments_user_FK foreign key(questionId) references Questions(questionId),
    constraint comment_user_FK foreign key(commentator) references users(id)
);

CREATE INDEX index_comment_createTime ON comment (createTime);

-- 通知表
-- id 通知 id
-- notifier 通知发起人
-- receiver 通知接收人
-- outerId 通知产生的地址，帖子或回复
-- type 消息类型 点赞or回复
create table notification
(
    id bigint PRIMARY KEY AUTO_INCREMENT,
    notifier bigint not null ,
    receiver bigint not null ,
    outerId bigint not null ,
    commentId bigint default -1,
    type int not null ,
    createTime bigint not null ,
    status int default 0,
    constraint notification_user_FK foreign key(notifier) references users(id),
    constraint notification_receiver_user_FK foreign key(receiver) references users(id)
);

-- 管理表,负责数据记录
create table admin
(
    adminId bigint primary key AUTO_INCREMENT,
    time bigint not null,
    questionCount bigint default 0,
    userCount bigint default 0,
    userAddCount bigint default 0,
    questionAddCount bigint default 0
);

--点赞表
-- likeId 点赞主键ID
-- notifier 点赞发起人
-- receiver 点赞接收人
-- questionId 点赞的问题ID
-- commentId 点赞的评论ID
-- type 点赞类型
-- createTime 点赞时间
create table clickLike
(
    likeId bigint primary key AUTO_INCREMENT,
    notifier bigint not null,
    notifierName varchar(50),
    receiver bigint not null,
    questionId bigint,
    commentId bigint,
    type bigint,
    createTime bigint,
    notificationId bigint
);

CREATE TABLE `topic`
(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `talkCount` bigint(20) DEFAULT 0,
  `followCount` int(100) DEFAULT 0,
  `image` varchar(10000) DEFAULT NULL,
  `simpleDesc` varchar(1000) DEFAULT NULL,
  `type` int(256) default null,
  `createTime` bigint(20) DEFAULT NULL,
  `modifiedTime` bigint(20) DEFAULT NULL,
  `status` int(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('JavaScript', '/image/topicicon/javascript.svg', 'JavaScript是一种直译式脚本语言，是一种动态类型、弱类型、基于原型的语言，内置支持类型。它的解释器被称为JavaScript引擎，为浏览器的一部分，广泛用于客户端的脚本语言，最早是在HTML（标准通用标记语言下的一个应用）网页上使用，用来给HTML网页增加动态功能。', '1', '1570610874840', '1570610874840');

insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('', '/image/topicicon/javascript.svg', '', '2', '1570610874840', '1570610874840');



insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('PHP', '/image/topicicon/php.svg', 'PHP即“超文本预处理器”，是一种通用开源脚本语言。PHP是在服务器端执行的脚本语言，与C语言类似，是常用的网站编程语言。PHP独特的语法混合了C、Java、Perl以及 PHP 自创的语法。利于学习，使用广泛，主要适用于Web开发领域。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Java', '/image/topicicon/java.svg', 'Java是一门面向对象编程语言，不仅吸收了C++语言的各种优点，还摒弃了C++里难以理解的多继承、指针等概念，因此Java语言具有功能强大和简单易用两个特征。Java语言作为静态面向对象编程语言的代表，极好地实现了面向对象理论，允许程序员以优雅的思维方式进行复杂的编程。Java具有简单性、面向对象、分布式、健壮性、安全性、平台独立与可移植性、多线程、动态性等特点。Java可以编写桌面应用程序、Web应用程序、分布式系统和嵌入式系统应用程序等', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('HTML', '/image/topicicon/html.svg', 'HTML称为超文本标记语言，是一种标识性的语言。它包括一系列标签．通过这些标签可以将网络上的文档格式统一，使分散的Internet资源连接为一个逻辑整体。HTML文本是由HTML命令组成的描述性文本，HTML命令可以说明文字，图形、动画、声音、表格、链接等。', '开发语言', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('CSS', '/image/topicicon/css.svg', '层叠样式表(英文全称：Cascading Style Sheets)是一种用来表现HTML（标准通用标记语言的一个应用）或XML（标准通用标记语言的一个子集）等文件样式的计算机语言。CSS不仅可以静态地修饰网页，还可以配合各种脚本语言动态地对网页各元素进行格式化。CSS 能够对网页中元素位置的排版进行像素级精确控制，支持几乎所有的字体字号样式，拥有对网页对象和模型样式编辑的能力。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Python', '/image/topicicon/python.svg', 'Python是一种跨平台的计算机程序设计语言。是一种面向对象的动态类型语言，最初被设计用于编写自动化脚本(shell)，随着版本的不断更新和语言新功能的添加，越来越多被用于独立的、大型项目的开发。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('C/C++', '/image/topicicon/cpp.svg', 'C++是C语言的继承，它既可以进行C语言的过程化程序设计，又可以进行以抽象数据类型为特点的基于对象的程序设计，还可以进行以继承和多态为特点的面向对象的程序设计。C++擅长面向对象程序设计的同时，还可以进行基于过程的程序设计，因而C++就适应的问题规模而论，大小由之。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('C#', '/image/topicicon/csharp.svg', 'C#是微软公司发布的一种面向对象的、运行于.NET Framework和.NET Core(完全开源，跨平台)之上的高级程序设计语言。并定于在微软职业开发者论坛(PDC)上登台亮相。C#是微软公司研究员Anders Hejlsberg的最新成果。C#看起来与Java有着惊人的相似；它包括了诸如单一继承、接口、与Java几乎同样的语法和编译成中间代码再运行的过程。但是C#与Java有着明显的不同，它借鉴了Delphi的一个特点，与COM（组件对象模型）是直接集成的，而且它是微软公司 .NET windows网络框架的主角。C#是一种安全的、稳定的、简单的、优雅的，由C和C++衍生出来的面向对象的编程语言。它在继承C和C++强大功能的同时去掉了一些它们的复杂特性（例如没有宏以及不允许多重继承）。C#综合了VB简单的可视化操作和C++的高运行效率，以其强大的操作能力、优雅的语法风格、创新的语言特性和便捷的面向组件编程的支持成为.NET开发的首选语言。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Go', '/image/topicicon/golang.svg', 'Go（又称 Golang）是 Google 的 Robert Griesemer，Rob Pike 及 Ken Thompson 开发的一种静态强类型、编译型语言。Go 语言语法与 C 相近，但功能上有：内存安全，GC（垃圾回收），结构形态及 CSP-style 并发计算。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Objective-C', '/image/topicicon/objectivec.svg', 'Objective-C，通常写作ObjC或OC和较少用的Objective C或Obj-C，是扩充C的面向对象编程语言。它主要使用于Mac OS X和GNUstep这两个使用OpenStep标准的系统，而在NeXTSTEP和OpenStep中它更是基本语言。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('TypeScript', '/image/topicicon/typescript.svg', 'TypeScript是一种由微软开发的自由和开源的编程语言。它是JavaScript的一个超集，而且本质上向这个语言添加了可选的静态类型和基于类的面向对象编程。安德斯·海尔斯伯格，C#的首席架构师，已工作于TypeScript的开发。TypeScript扩展了JavaScript的语法，所以任何现有的JavaScript程序可以不加改变的在TypeScript下工作。TypeScript是为大型应用的开发而设计，而编译它时产生 JavaScript 以确保兼容性。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('shell', '/image/topicicon/shell.svg', '计算机科学中，Shell俗称壳（用来区别于核），是指“为使用者提供操作界面”的软件（命令解析器）。它类似于DOS下的command.com和后来的cmd.exe。它接收用户命令，然后调用相应的应用程序。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Swift', '/image/topicicon/Swift.svg', 'Swift 是一种支持多编程范式和编译式的开源编程语言,苹果于2014年WWDC（苹果开发者大会）发布，用于开发 iOS，OS X 和 watchOS 应用程序。Swift 结合了 C 和 Objective-C 的优点并且不受 C 兼容性的限制。Swift 在 Mac OS 和 iOS 平台可以和 Object-C 使用相同的运行环境。2015年6月8日，苹果于WWDC 2015上宣布，Swift将开放源代码，包括编译器和标准库。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Ruby', '/image/topicicon/Ruby.svg', 'Ruby，一种简单快捷的面向对象（面向对象程序设计）脚本语言，在20世纪90年代由日本人松本行弘(Yukihiro Matsumoto)开发，遵守GPL协议和Ruby License。它的灵感与特性来自于 Perl、Smalltalk、Eiffel、Ada以及 Lisp 语言。由 Ruby 语言本身还发展出了JRuby（Java平台）、IronRuby（.NET平台）等其他平台的 Ruby 语言替代品。Ruby的作者于1993年2月24日开始编写Ruby，直至1995年12月才正式公开发布于fj（新闻组）。因为Perl发音与6月诞生石pearl（珍珠）相同，因此Ruby以7月诞生石ruby（红宝石）命名。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Lua', '/image/topicicon/lua.svg', 'Lua 是一个小巧的脚本语言。它是巴西里约热内卢天主教大学（Pontifical Catholic University of Rio de Janeiro）里的一个由Roberto Ierusalimschy、Waldemar Celes 和 Luiz Henrique de Figueiredo三人所组成的研究小组于1993年开发的。 其设计目的是为了通过灵活嵌入应用程序中从而为应用程序提供灵活的扩展和定制功能。Lua由标准C编写而成，几乎在所有操作系统和平台上都可以编译，运行。Lua并没有提供强大的库，这是由它的定位决定的。所以Lua不适合作为开发独立应用程序的语言。Lua 有一个同时进行的JIT项目，提供在特定平台上的即时编译功能。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Scala', '/image/topicicon/scala.svg', 'Scala 是一门多范式（multi-paradigm）的编程语言，设计初衷是要集成面向对象编程和函数式编程的各种特性。Scala 运行在Java虚拟机上，并兼容现有的Java程序。Scala 源代码被编译成Java字节码，所以它可以运行于JVM之上，并可以调用现有的Java类库。', '开发语言', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Kotlin', '/image/topicicon/Kotlin.jpg', 'Kotlin 是一个用于现代多平台应用的静态编程语言，由 JetBrains 开发。Kotlin可以编译成Java字节码，也可以编译成JavaScript，方便在没有JVM的设备上运行。Kotlin已正式成为Android官方支持开发语言。', '1', '1570610874840', '1570610874840');



insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Spring', '/image/topicicon/spring.svg', 'Spring是一个轻量级控制反转(IoC)和面向切面(AOP)的容器框架。具体查看Spring官网<a href="https://spring.io" target="_blank">Spring</a>', '2', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Django', '/image/topicicon/django.svg', 'Django是一个开放源代码的Web应用框架，由Python写成。采用了MTV的框架模式，即模型M，视图V和模版T。它最初是被开发来用于管理劳伦斯出版集团旗下的一些以新闻内容为主的网站的，即是CMS（内容管理系统）软件。并于2005年7月在BSD许可证下发布。这套框架是以比利时的吉普赛爵士吉他手Django Reinhardt来命名的。<a href="https://www.djangoproject.com" target="_blank">Django</a>', '2', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Flask', '/image/topicicon/flask.jpg', 'Flask是一个使用 Python 编写的轻量级 Web 应用框架。其 WSGI 工具箱采用 Werkzeug ，模板引擎则使用 Jinja2 。Flask使用 BSD 授权。Flask也被称为 “microframework” ，因为它使用简单的核心，用 extension 增加其他功能。Flask没有默认使用的数据库、窗体验证工具。', '2', '1570610874840', '1570610874840');









--alter table Questions modify questionId bigint NOT NULL;
--alter table users modify id bigint NOT NULL;
--select * from Questions limit 0, 5;
--alter table userPermission add updateTime varchar(19) not null ;
--alter table userPermission change  column modifer modifier varchar(20)
--insert into userPermission values (1,0);

