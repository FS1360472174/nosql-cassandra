# nosql-cassandra
cassandra 知识

Q1.cassandra 能不能使用 select count(*) from table

A1.理论上是不可以，NoSql 面向的是大数据，count(*)需要访问数据所有的节点，进行全局扫描，然后聚合，
在大数据下，非常耗时。所以不可以做这样的操作

Q2.一个节点down掉，对已有的数据表有何影响，对新创建的数据表有何影响


Q3.为什么cassandra不建议使用root用户运行cassandra service

