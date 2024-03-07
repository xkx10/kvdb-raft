# kvdb-raft

## 项目背景

本项目主要是实现一个开箱即用分布式的KV数据库，该数据库主要有以下特点

- 基于Raft分布式共识算法实现
- 做到强一致性


> 这个项目主要为了促进自己学习，也为了2025年的校园招聘会做准备。


## 项目文档
项目文档链接：[https://www.yuque.com/g/u960127/kb/lucpa54nwageannt/collaborator/join?token=Bdrgr0PwyULmnVU4&source=doc_collaborator# 《系分文档1期》](https://www.yuque.com/g/u960127/kb/lucpa54nwageannt/collaborator/join?token=Bdrgr0PwyULmnVU4&source=doc_collaborator)

## 项目架构图
![yuque_diagram.jpg](image%2Fyuque_diagram.jpg)


## Getting started
准备工作：
- redis
- zookeeper
> 也可通过docker快速启动：
1. redis docker run -d --name myredis -p 6379:6379 redis --requirepass "123456"
2. docker run --name some-zookeeper -p 2181:2181 --restart always -d zookeeper
```
git clone https://github.com/xkx10/kvdb-raft
启动三个服务分别配置activate
-Dspring.profiles.active=node1
-Dspring.profiles.active=node2
-Dspring.profiles.active=node3
```

