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

Q5.集群环境，insert 操作的throughput刚开始很高，3,4分钟后下降的很厉害。一段时间后throughput基本为0了。
A5:INSERT 操作throughput 很低的时候，cluster的cpu很低，disk IO 也很低。应该是有其他操作block住了write request.
一开始怀疑client 端的connection pool设置有问题，占用了太多资源，排查不是。
怀疑GC 有问题，排查不是。
怀疑mem flush 问题，排查不是。
最终发现是compaction问题，默认compaction是16-32 倍的write 速度。但是目前的集群环境能力受限，8G RAM,compaction strategy 是SIZE 方式，同样耗费内存。所以问题出在了compaction，以目前的write rate 写入数据，和16 倍的write 速度 的compaction，集群承受不住，所以都去compaction了，没有资源去write。
将compaction 速度设置为2 倍，延迟compcation，发现不会出现上述问题。
另外一个方面说明集群不具备处理当前这样的插入负载，所以应该调低插入的速度，测试得到目前环境多能承载的负载

Q6.节点down掉后，重新启动状态为UJ，而不是UN

A6:
nodetool netstats可以看到seed node 在发送文件到当前节点。
类似ma-46-big-Data.db  这样的文件。文件数目很多，造成节点还是在join 状态

Q7:local_one,local_quorum 对于多数据中心，应用程序是如何来判断是本地数据中心。
需要程序来指明吗

Q8:cassandra 每个节点都知道其他节点的数据分布情况。
是对每个table的在每个节点上都维护一个map表，还是直接根据hash值来判断。

如果是根据hash值，那么加入新节点，新节点获取新key，将原有节点的数据copy过来，然后将旧的节点key值清掉。
问题是如果复制因子是3.
![image](https://github.com/FS1360472174/nosql-cassandra/blob/master/ring.PNG)

原来有份数据在4,5,1节点(simplystrategy) 4作为数据的一个副本
现在添加了节点6，4的部分数据移到6上去，数据的第一个副本移到了6上去了。
数据的其他两个副本在5,1上面，这时候如何知道数据的其他两个副本呢

Q9.一个decommissed 的节点重新加入和一个新节点加入有什么区别
