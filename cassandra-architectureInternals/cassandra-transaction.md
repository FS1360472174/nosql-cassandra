# Cassandra事务与关系型数据库事务有何区别 #

Cassandra不会使用回滚和锁机制来实现关系型数据的ACID事务，相比较于提供原子性，隔离性和持久化，Cassandra提供最终(可调节的)一致性，让用户决定为每个事务提供强一致性或者最终一致性。

作为非关系型数据库，Cassandra不支持join或者外键，因此Cassandra不提供ACID层面的一致性。例如，当从账户A转账给账户B，整个账户总额不应该改变。Cassandra支持row-level的原子性和隔离性，为了提供高可用和更快的写入性能，牺牲了事务的隔离性和原子性。Cassandra写操作是持久化的。

## 原子性 ##

在Cassandra中，写入操作时partition level的原子性，意味着同一分区的2行或者多行的写入或者更新被当做同一写入操作。删除操作同样是partition level。

例如，如果写一致性为QUORUM,relication factor为3，Cassandra会将写操作复制到集群中的所有的节点，然后等待2个节点的应答。如果某个节点写入失败了但是其他节点成功了，Cassandra会在失败的节点报告失败。然而，其他成功写入的节点不会自动进行回滚。

Cassandra使用客户端的时间戳来决定一列的最新更新。当请求数据的时候，最新的时间戳赢，因此如果多个客户端会话同时更新一行的相同列，最后更新的才会被读操作看到。

## 隔离性 ##

Cassandra 写和删除操作是完全行level的隔离性。这意味着在单个节点上的一个分区，对客户端来说一次只能写入一行。这个操作范围是严格受限的，直到他完成。在一个batch操作中的所有更新，属于同一个给定的partition key有一样的限制。然而，如果batch操作中包含超过一个分区的更新，并不是隔离的。

## 持久化 ##

Cassandra中的写操作是持久化的。一个节点上的所有写操作在收到应答标记写入成功之前都会写入到内存和磁盘的commit log中。如果在memtables flush到磁盘之前，忽然宕机或者节点失败，commit log 可以用来在节点恢复重启时找回丢失的写入操作。除了本地的持久化(数据立即写入到磁盘中)，其他节点上存有副本也增强了持久化。

可以通过使用[commitlog_sync](http://docs.datastax.com/en/cassandra/3.0/cassandra/configuration/configCassandra_yaml.html#configCassandra_yaml__commitlog_sync)选项来管理本地的持久化，满足对于一致性的需求。设置选项为periodic 或者batch