-- 创建数据库
create database community;

--查看数据库编码
show variables LIKE 'collation_%';

--如果不是utf8 collation_database 则修改为 utf8 编码
--修改数据库编码属性
ALTER DATABASE oj CHARACTER SET utf8 COLLATE utf8_unicode_ci;

--创建用户表
--创建用户表
CREATE TABLE users
(
    id INT,
    userName VARCHAR(20) NOT NULL,
    password VARCHAR(60) NOT NULL,
    email VARCHAR(30) NOT NULL,
    sex VARCHAR(2) NOT NULL,
    age int,
    birthday varchar(19),
    school varchar(20),
    careatTime VARCHAR(19),
    lastTime VARCHAR(19),
    headUrl VARCHAR(30),
    PRIMARY KEY (id)
);

--设置用户名不重复
ALTER TABLE community.users ADD UNIQUE (userName);
--设置邮箱不重复
ALTER TABLE community.users ADD UNIQUE (email);

--设置主键自增
--alter table users modify userId int auto_increment;
--设置主键从1000开始自增
alter table users modify id int AUTO_INCREMENT=1000;