-- 创建数据库
create database community;
use community;

-- 查看数据库编码
show variables LIKE 'collation_%';

-- 如果不是utf8mb4 collation_database 则修改为 utf8mb4 编码
-- 修改数据库编码属性
ALTER DATABASE community CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- TODO 修改时间为bigint 方便后期排序

-- 创建用户表
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
    followCount bigint default 0,
    fansCount bigint default 0,
    creationTime VARCHAR(19),
    lastTime VARCHAR(19),
    headUrl VARCHAR(300),
    userTopPhotoUrl varchar (300),
    PRIMARY KEY (id)
);

ALTER TABLE community.users ADD UNIQUE (userId);
-- 设置邮箱不重复
ALTER TABLE community.users ADD UNIQUE (email);

-- 设置主键自增
alter table users modify id bigint auto_increment;
-- 设置主键从1000开始自增
alter table users AUTO_INCREMENT=1000;


-- 用户权限表
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

-- 在线用户表
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

-- 问题表
CREATE TABLE Questions
(
    questionId bigint PRIMARY KEY AUTO_INCREMENT,
    userId bigint NOT NULL,
    title varchar(60),
    classification varchar (50),
    description LONGTEXT,
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
    parentCommentId bigint not null,
    parentId bigint NOT NULL,
    type int NOT NULL,
    commentator bigint NOT NULL,
    content LONGTEXT NOT NULL,
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

-- 点赞表
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
  `type` int default null,
  `createTime` bigint(20) DEFAULT NULL,
  `modifiedTime` bigint(20) DEFAULT NULL,
  `status` int(2) DEFAULT '1',
  PRIMARY KEY (`id`)
);

-- insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('', '/image/topicicon/javascript.svg', '', '7', '1570610874840', '1570610874840');


insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('JavaScript', '/image/topicicon/javascript.svg', 'JavaScript是一种直译式脚本语言，是一种动态类型、弱类型、基于原型的语言，内置支持类型。它的解释器被称为JavaScript引擎，为浏览器的一部分，广泛用于客户端的脚本语言，最早是在HTML（标准通用标记语言下的一个应用）网页上使用，用来给HTML网页增加动态功能。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('PHP', '/image/topicicon/php.svg', 'PHP即“超文本预处理器”，是一种通用开源脚本语言。PHP是在服务器端执行的脚本语言，与C语言类似，是常用的网站编程语言。PHP独特的语法混合了C、Java、Perl以及 PHP 自创的语法。利于学习，使用广泛，主要适用于Web开发领域。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Java', '/image/topicicon/java.svg', 'Java是一门面向对象编程语言，不仅吸收了C++语言的各种优点，还摒弃了C++里难以理解的多继承、指针等概念，因此Java语言具有功能强大和简单易用两个特征。Java语言作为静态面向对象编程语言的代表，极好地实现了面向对象理论，允许程序员以优雅的思维方式进行复杂的编程。Java具有简单性、面向对象、分布式、健壮性、安全性、平台独立与可移植性、多线程、动态性等特点。Java可以编写桌面应用程序、Web应用程序、分布式系统和嵌入式系统应用程序等', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('HTML', '/image/topicicon/html.svg', 'HTML称为超文本标记语言，是一种标识性的语言。它包括一系列标签．通过这些标签可以将网络上的文档格式统一，使分散的Internet资源连接为一个逻辑整体。HTML文本是由HTML命令组成的描述性文本，HTML命令可以说明文字，图形、动画、声音、表格、链接等。', '1', '1570610874840', '1570610874840');
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
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Scala', '/image/topicicon/scala.svg', 'Scala 是一门多范式（multi-paradigm）的编程语言，设计初衷是要集成面向对象编程和函数式编程的各种特性。Scala 运行在Java虚拟机上，并兼容现有的Java程序。Scala 源代码被编译成Java字节码，所以它可以运行于JVM之上，并可以调用现有的Java类库。', '1', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Kotlin', '/image/topicicon/Kotlin.jpg', 'Kotlin 是一个用于现代多平台应用的静态编程语言，由 JetBrains 开发。Kotlin可以编译成Java字节码，也可以编译成JavaScript，方便在没有JVM的设备上运行。Kotlin已正式成为Android官方支持开发语言。', '1', '1570610874840', '1570610874840');



insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Spring', '/image/topicicon/spring.svg', 'Spring是一个轻量级控制反转(IoC)和面向切面(AOP)的容器框架。具体查看Spring官网<a href="https://spring.io" target="_blank">Spring</a>', '2', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Django', '/image/topicicon/django.svg', 'Django是一个开放源代码的Web应用框架，由Python写成。采用了MTV的框架模式，即模型M，视图V和模版T。它最初是被开发来用于管理劳伦斯出版集团旗下的一些以新闻内容为主的网站的，即是CMS（内容管理系统）软件。并于2005年7月在BSD许可证下发布。这套框架是以比利时的吉普赛爵士吉他手Django Reinhardt来命名的。<a href="https://www.djangoproject.com" target="_blank">Django</a>', '2', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Flask', '/image/topicicon/flask.jpg', 'Flask是一个使用 Python 编写的轻量级 Web 应用框架。其 WSGI 工具箱采用 Werkzeug ，模板引擎则使用 Jinja2 。Flask使用 BSD 授权。Flask也被称为 “microframework” ，因为它使用简单的核心，用 extension 增加其他功能。Flask没有默认使用的数据库、窗体验证工具。', '2', '1570610874840', '1570610874840');



insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Linux', '/image/topicicon/linux.svg', 'Linux是一套免费使用和自由传播的类Unix操作系统，是一个基于POSIX和Unix的多用户、多任务、支持多线程和多CPU的操作系统。它能运行主要的Unix工具软件、应用程序和网络协议。它支持32位和64位硬件。Linux继承了Unix以网络为核心的设计思想，是一个性能稳定的多用户网络操作系统。', '3', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Nginx', '/image/topicicon/NGINX.svg', 'Nginx (engine x) 是一个高性能的HTTP和反向代理web服务器，同时也提供了IMAP/POP3/SMTP服务。Nginx是由伊戈尔·赛索耶夫为俄罗斯访问量第二的Rambler.ru站点（俄文：Рамблер）开发的，第一个公开版本0.1.0发布于2004年10月4日。', '3', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Docker', '/image/topicicon/Docker.svg', 'Docker 是一个开源的应用容器引擎，让开发者可以打包他们的应用以及依赖包到一个可移植的镜像中，然后发布到任何流行的 Linux或Windows 机器上，也可以实现虚拟化。容器是完全使用沙箱机制，相互之间不会有任何接口。', '3', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Ubuntu', '/image/topicicon/ubuntu.svg', 'Ubuntu（又称乌班图）是一个以桌面应用为主的开源GNU/Linux操作系统，Ubuntu 是基于Debian GNU/Linux，支持x86、amd64（即x64）、ARM和ppc架构，由全球化的专业开发团队（Canonical Ltd）打造的。', '3', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('CentOS', '/image/topicicon/centos.svg', 'CentOS（Community Enterprise Operating System，中文意思是社区企业操作系统）是Linux发行版之一，它是来自于Red Hat Enterprise Linux依照开放源代码规定释出的源代码所编译而成。由于出自同样的源代码，因此有些要求高度稳定性的服务器以CentOS替代商业版的Red Hat Enterprise Linux使用。两者的不同，在于CentOS完全开源', '3', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Apache', '/image/topicicon/Apache.svg', 'Apache是世界使用排名第一的Web服务器软件。它可以运行在几乎所有广泛使用的计算机平台上，由于其跨平台和安全性被广泛使用，是最流行的Web服务器端软件之一。它快速、可靠并且可通过简单的API扩充，将Perl/Python等解释器编译到服务器中。', '3', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Windows Server', '/image/topicicon/Windows.svg', 'Windows Server是微软在2003年4月24日推出的Windows 的服务器操作系统，其核心是Microsoft Windows Server System（WSS），每个Windows Server都与其家用（工作站）版对应（2003 R2除外）。', '3', '1570610874840', '1570610874840');


insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('MySQL', '/image/topicicon/mysql.svg', 'MySQL是一个关系型数据库管理系统，由瑞典MySQL AB 公司开发，目前属于 Oracle 旗下产品。MySQL 是最流行的关系型数据库管理系统之一，在 WEB 应用方面，MySQL是最好的 RDBMS (Relational Database Management System，关系数据库管理系统) 应用软件之一。', '4', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Redis', '/image/topicicon/redis.svg', 'Redis（全称：Remote Dictionary Server 远程字典服务）是一个开源的使用ANSI C语言编写、支持网络、可基于内存亦可持久化的日志型、Key-Value数据库，并提供多种语言的API。从2010年3月15日起，Redis的开发工作由VMware主持。从2013年5月开始，Redis的开发由Pivotal赞助。', '4', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('MongoDB', '/image/topicicon/MongoDB.svg', 'MongoDB 是一个基于分布式文件存储的数据库。由 C++ 语言编写。旨在为 WEB 应用提供可扩展的高性能数据存储解决方案。MongoDB 是一个介于关系数据库和非关系数据库之间的产品，是非关系数据库当中功能最丰富，最像关系数据库的。', '4', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('NoSQL', '/image/topicicon/NoSQL.svg', 'NoSQL 泛指非关系型的数据库。随着互联网web2.0网站的兴起，传统的关系数据库在处理web2.0网站，特别是超大规模和高并发的SNS类型的web2.0纯动态网站已经显得力不从心，出现了很多难以克服的问题，而非关系型的数据库则由于其本身的特点得到了非常迅速的发展。NoSQL数据库的产生就是为了解决大规模数据集合多重数据种类带来的挑战，尤其是大数据应用难题。', '4', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('SQLite', '/image/topicicon/sqlite370_banner.gif', 'SQLite，是一款轻型的数据库，是遵守ACID的关系型数据库管理系统，它包含在一个相对小的C库中。它是D.RichardHipp建立的公有领域项目。它的设计目标是嵌入式的，而且目前已经在很多嵌入式产品中使用了它，它占用资源非常的低，在嵌入式设备中，可能只需要几百K的内存就够了。', '4', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Oracle', '/image/topicicon/oracle-online.svg', 'Oracle Database，又名Oracle RDBMS，或简称Oracle。是甲骨文公司的一款关系数据库管理系统。它是在数据库领域一直处于领先地位的产品。可以说Oracle数据库系统是目前世界上流行的关系数据库管理系统，系统可移植性好、使用方便、功能强，适用于各类大、中、小、微机环境。它是一种高效率、可靠性好的、适应高吞吐量的数据库方案。', '4', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('SQL Server', '/image/topicicon/SQLServer.svg', 'Microsoft SQL Server 是一个全面的数据库平台，使用集成的商业智能 (BI)工具提供了企业级的数据管理。Microsoft SQL Server 数据库引擎为关系型数据和结构化数据提供了更安全可靠的存储功能，使您可以构建和管理用于业务的高可用和高性能的数据应用程序。', '4', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('H2', '/image/topicicon/H2.svg', 'H2是Thomas Mueller提供的一个开源的、纯java实现的关系数据库。H2是一个开源的嵌入式数据库引擎，采用java语言编写，不受平台的限制，同时H2提供了一个十分方便的web控制台用于操作和管理数据库内容。H2还提供兼容模式，可以兼容一些主流的数据库，因此采用H2作为开发期的数据库非常方便。H2最大的用途在于可以同应用程序打包在一起发布，这样可以非常方便地存储少量结构化数据。', '4', '1570610874840', '1570610874840');


insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Git', '/image/topicicon/Git.svg', 'Git(读音为/gɪt/。)是一个开源的分布式版本控制系统，可以有效、高速地处理从很小到非常大的项目版本管理。Git 是 Linus Torvalds 为了帮助管理 Linux 内核开发而开发的一个开放源码的版本控制软件。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('VS Code', '/image/topicicon/VSCode.svg', 'Visual Studio Code (简称 VS Code / VSC) 是一款免费开源的现代化轻量级代码编辑器，支持几乎所有主流的开发语言的语法高亮、智能代码补全、自定义快捷键、括号匹配和颜色区分、代码片段、代码对比 Diff、GIT命令 等特性，支持插件扩展，并针对网页开发和云端应用开发做了优化。软件跨平台支持 Win、Mac 以及 Linux，运行流畅，可谓是微软的良心之作', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Visual Studio', '/image/topicicon/visual-studio.svg', 'Microsoft Visual Studio（简称VS）是美国微软公司的开发工具包系列产品。VS是一个基本完整的开发工具集，它包括了整个软件生命周期中所需要的大部分工具，如UML工具、代码管控工具、集成开发环境(IDE)等等。所写的目标代码适用于微软支持的所有平台，包括Microsoft Windows、Windows Mobile、Windows CE、.NET Framework、.NET Compact Framework和Microsoft Silverlight 及Windows Phone。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Vim', '/image/topicicon/vim.svg', 'Vim是一个类似于Vi的著名的功能强大、高度可定制的文本编辑器，在Vi的基础上改进和增加了很多特性。VIM是自由软件。Vim普遍被推崇为类Vi编辑器中最好的一个。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('IntelliJ IDEA', '/image/topicicon/idea.png', 'IDEA 全称 IntelliJ IDEA，是java编程语言开发的集成环境。IntelliJ在业界被公认为最好的java开发工具之一，尤其在智能代码助手、代码自动提示、重构、J2EE支持、各类版本工具(git、svn等)、JUnit、CVS整合、代码分析、 创新的GUI设计等方面的功能可以说是超常的。IDEA是JetBrains公司的产品，这家公司总部位于捷克共和国的首都布拉格，开发人员以严谨著称的东欧程序员为主。它的旗舰版本还支持HTML，CSS，PHP，MySQL，Python等。免费版只支持Python等少数语言。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Eclipse', '/image/topicicon/logo-eclipse.png', 'Eclipse 是一个开放源代码的、基于Java的可扩展开发平台。就其本身而言，它只是一个框架和一组服务，用于通过插件组件构建开发环境。幸运的是，Eclipse 附带了一个标准的插件集，包括Java开发工具（Java Development Kit，JDK）。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Maven', '/image/topicicon/maven-logo.png', 'Maven项目对象模型(POM)，可以通过一小段描述信息来管理项目的构建，报告和文档的项目管理工具软件。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Gradle', '/image/topicicon/Gradle.svg', 'Gradle是一个基于Apache Ant和Apache Maven概念的项目自动化构建开源工具。它使用一种基于Groovy的特定领域语言(DSL)来声明项目设置，目前也增加了基于Kotlin语言的kotlin-based DSL，抛弃了基于XML的各种繁琐配置。面向Java应用为主。当前其支持的语言限于Java、Groovy、Kotlin和Scala，计划未来将支持更多的语言。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('NetBeans', '/image/topicicon/logo_netbeans_red.png', 'NetBeans是Sun公司（2009年被甲骨文收购）在2000年创立的开放源代码供开发人员和客户社区的家园，旨在构建世界级的Java IDE。NetBeans当前可以在Solaris、Windows、Linux和Macintosh OS X平台上进行开发，并在SPL(Sun公用许可)范围内使用。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('PyCharm', '/image/topicicon/pycharm.svg', 'PyCharm是一种Python IDE，带有一整套可以帮助用户在使用Python语言开发时提高其效率的工具，比如调试、语法高亮、Project管理、代码跳转、智能提示、自动完成、单元测试、版本控制。此外，该IDE提供了一些高级功能，以用于支持Django框架下的专业Web开发。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Navicat', '/image/topicicon/navicat.jpg', 'Navicat是一套快速、可靠并价格相当便宜的数据库管理工具，专为简化数据库的管理及降低系统管理成本而设。它的设计符合数据库管理员、开发人员及中小企业的需要。Navicat 是以直觉化的图形用户界面而建的，让你可以以安全并且简单的方式创建、组织、访问并共用信息。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('CLion', '/image/topicicon/clion.svg', ' CLion是一款专为开发C及C++所设计的跨平台IDE。它是以IntelliJ为基础设计的,包含了许多智能功能来提高开发人员的生产力。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('WebStorm', '/image/topicicon/WebStorm.svg', 'WebStorm 是jetbrains公司旗下一款JavaScript 开发工具。目前已经被广大中国JS开发者誉为“Web前端开发神器”、“最强大的HTML5编辑器”、“最智能的JavaScript IDE”等。与IntelliJ IDEA同源，继承了IntelliJ IDEA强大的JS部分的功能。', '5', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('PhpStorm', '/image/topicicon/PhpStorm.svg', 'PhpStorm 是 JetBrains 公司开发的一款商业的 PHP 集成开发工具，旨在提高用户效率，可深刻理解用户的编码，提供智能代码补全，快速导航以及即时错误检查。', '5', '1570610874840', '1570610874840');




insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Android', '/image/topicicon/Android.svg', '安卓（Android）是一种基于Linux的自由及开放源代码的操作系统。主要使用于移动设备，如智能手机和平板电脑，由Google公司和开放手机联盟领导及开发。Android操作系统最初由Andy Rubin开发，主要支持手机。2005年8月由Google收购注资。', '6', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('Windows', '/image/topicicon/Windows.svg', 'Windows操作系统是最常见的计算机操作系统，是微软公司开发的操作软件。该软件经历了多年的发展历程，目前推出的win10系统相当成熟。Windows操作系统具有人机操作互动性好，支持应用软件多，硬件适配性强等特点。操作系统是将人类利用计算机硬件发挥作用的平台，是计算机软件运行工作的环境，是计算机硬件的翻译。从计算机诞生发展到今天，出现了相当多种类的操作系统。Windows操作系统是其中的佼佼者。Windows操作系统是美国微软公司推出的一款操作系统。该系统从1985年诞生到现在，经过多年的发展完善，相对比较成熟稳定，是当前个人计算机的主流操作系统。', '6', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('IOS', '/image/topicicon/ios.svg', 'iOS 是由苹果公司开发的移动操作系统。苹果公司最早于 2007 年 1 月 9 日的 Macworld 大会上公布这个系统，最初是设计给 iPhone 使用的，后来陆续套用到 iPod touch 、iPad 以及 Apple TV 等产品上。iOS与苹果的 macOS 操作系统一样，属于类Unix的商业操作系统。', '6', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('MacOS', '/image/topicicon/mac-os.svg', 'Mac OS是一套运行于苹果Macintosh系列电脑上的操作系统。Mac OS是首个在商用领域成功的图形用户界面操作系统。', '6', '1570610874840', '1570610874840');




insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('体育', '/image/topicicon/sports.svg', '体育', '7', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('娱乐八卦', '/image/topicicon/game.svg', '娱乐八卦', '7', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('闲聊', '/image/topicicon/chat.svg', '闲聊', '7', '1570610874840', '1570610874840');
insert into topic (title, image, simpleDesc, type, createTime, modifiedTime) values ('游戏', '/image/topicicon/games.svg', '游戏', '7', '1570610874840', '1570610874840');


-- 关注话题表
create table followTopic
(
    `followTopicId` bigint NOT NULL AUTO_INCREMENT,
    `topicId` bigint not null ,
    `topicTitle` varchar (100) not null ,
    `topicImage` varchar (10000),
    `userId` bigint not null ,
    `createTime` bigint not null ,
    PRIMARY KEY (`followTopicId`)
);
CREATE INDEX index_followTopic_userId ON followTopic (userId);

-- 关注问题
create table followQuestion
(
    `id` bigint not null AUTO_INCREMENT,
    `followQuestionId` bigint not null ,
    `followQuestionAuthor` bigint not null ,
    `followQuestionTitle` varchar(60) not null ,
    `userId` bigint not null ,
    `createTime` bigint not null ,
    PRIMARY KEY (`id`)
);
CREATE INDEX index_followQuestion_userId ON followQuestion (userId);
CREATE INDEX index_followQuestion_questionId ON followQuestion (followQuestionId);

-- 关注用户
create table followUser
(
    `id` bigint not null AUTO_INCREMENT,
    `followUserId` bigint not null ,
    `userId` bigint not null ,
    `createTime` bigint not null,
    PRIMARY KEY (`id`)
)
CREATE INDEX index_followUser_userId ON followUser (userId);
CREATE INDEX index_followUser_followUserId ON followUser (followUserId);


-- alter table Questions modify questionId bigint NOT NULL;
-- alter table users modify id bigint NOT NULL;
-- select * from Questions limit 0, 5;
-- alter table userPermission add updateTime varchar(19) not null ;
-- alter table userPermission change  column modifer modifier varchar(20)
-- insert into userPermission values (1,0);

