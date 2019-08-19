-- 创建数据库
create database community;
use community;

--查看数据库编码
show variables LIKE 'collation_%';

--如果不是utf8 collation_database 则修改为 utf8 编码
--修改数据库编码属性
ALTER DATABASE oj CHARACTER SET utf8 COLLATE utf8_unicode_ci;


--创建用户表
CREATE TABLE users
(
    id INT,
    userID VARCHAR(20) NOT NULL,
    userName VARCHAR(20) NOT NULL,
    password VARCHAR(60) NOT NULL,
    email VARCHAR(30) NOT NULL,
    sex VARCHAR(2) NOT NULL,
    age int,
    birthday varchar(19),
    school varchar(20),
    major varchar (20),
    selfIntroduction varchar (50),
    simpleSelfIntroduction varchar (100),
    likes varchar (50),
    creationTime VARCHAR(19),
    lastTime VARCHAR(19),
    headUrl VARCHAR(30),
    PRIMARY KEY (id)
);
--
ALTER TABLE community.users ADD UNIQUE (userId);
--设置邮箱不重复
ALTER TABLE community.users ADD UNIQUE (email);

--设置主键自增
alter table users modify id int auto_increment;
--设置主键从1000开始自增
alter table users AUTO_INCREMENT=1000;

alter table users add major varchar(20);
alter table users add userId varchar(100) not null;

--用户权限表
create table userPermission
(
    id int,
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
    id int,
    userName VARCHAR(20) NOT NULL,
    token varchar (100) not null ,
    ip varchar (128) not null ,
    time varchar (19) not null ,
    PRIMARY KEY (id),
    constraint onlineUser_FK foreign key(id) references users(id)
);

--问题表
CREATE TABLE Questions
(
    questionId int PRIMARY KEY AUTO_INCREMENT,
    userId int NOT NULL,
    title varchar(50),
    classification varchar (50),
    description text,
    fileUrl text,
    viewCount int DEFAULT 1,
    commentCount int DEFAULT 0,
    likeCount int DEFAULT 0,
    tag varchar(256),
    createTime varchar(19),
    alterTime varchar(19),
    constraint user_question_FK foreign key(userId) references users(id)
);

select * from Questions limit 0, 5;
--alter table userPermission add updateTime varchar(19) not null ;
--alter table userPermission change  column modifer modifier varchar(20)
--insert into userPermission values (1,0);

