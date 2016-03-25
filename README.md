# nosql-cassandra
cassandra 知识

Q1.cassandra 能不能使用 select count(*) from table

A1.理论上是不可以，NoSql 面向的是大数据，count(*)需要访问数据所有的节点，进行全局扫描，然后聚合，
在大数据下，非常耗时。所以不可以做这样的操作

Q2.一个节点down掉，对已有的数据表有何影响，对新创建的数据表有何影响


Q3.为什么cassandra不建议使用root用户运行cassandra service

Q4.node 因为out-of-memory down了，整个cluster无法工作。重新启动不成功

A4.
1.rm -rf /data/system/*
2.加上Dcassandra.join_ring=false
启动参数，
3.kill 掉，重新启动，然后去掉Dcassandra.join_ring=false参数。

2,3 步骤没有什么理论支持，只是尝试了这么做，发现可以启动。
从system.log 看到的启动log，出错是在recovery commitlog 这步，write out of time.

