# 如何利用线性一致性来实现轻量级的事务 #

分布式数据库会面临着一个独特的挑战，就是数据必须要严格的按照读，写顺序执行。如创建用户，转账，两个潜在的写操作竞态条件必须要确保一个写操作必须在另外一个之前发生。在Cassandra中，使用Paxos协议来实现轻量级的事务来处理并发操作。

Paxos协议是用来实现线性一致性，这是实时约束的顺序一致性。线性一致性保证事务的隔离性类似于RDBMS提供的串行level的隔离性。这种类型的事务就是众所周知的compare and set(CAS)；副本数据被拿来进行比较，发现有任何数据过期了，就设置为最新的数据。在Cassandra中，这个过程融合了Paxos协议和正常的读写操作来实现compare and set操作。

Paxos 协议实现包含一系列阶段

1. 准备/承诺阶段
2. 读取/结果阶段
3. 提议/接受阶段
4. 提交/应答阶段

这些阶段的动作发生在一个提议者和多个接收者。任何节点都可以成为一个提议者，在同一时间内，多个提议者可以同时发生。为了简单起见，下面的描述只使用一个提议者。Proposer准备阶段，发送一个包含proposal序号的信息给quorum个接受者。每个接受者承诺接受proposal，如果proposal序号是它们接收到最大的那个。一旦proposer接收到了quorum个acceptors的承诺。从每个acceptor中读取到的值会返回给proposer。proposer会计算出值，然后将值和proposal序号一起发送给quorum个acceptors。每个acceptor接受到了一个特定序号的proposal,就会承诺不再接受小数值的proposal。如果所有的条件都满足了，这个值会被提交和作为cassandra写操作的应答。

这四个阶段需要在提议轻量级事务的节点和涉及到事务的如何集群事务节点之间经过4轮请求应答。性能会受到影响，因此，为并发场景保留轻量级事务需要仔细考虑。

轻量级事务会阻塞其他轻量级事务的发生，但是不同阻止正常的读写操作发生。轻量级事务使用时间戳机制与正常的操作进行区分，将轻量级事务与正常的操作混合在一起，可能会产生错误。如果轻量级事务被用来写入分区内的某行，只能用于读和写。对于所有的操作都必须要小心，不过是单个或者是批量。例如，下面这样的操作场景就会失败:

    DELETE ...
	INSERT .... IF NOT EXISTS
	SELECT ....

下面的一系列操作会工作

	DELETE ... IF EXISTS
	INSERT .... IF NOT EXISTS
	SELECT .....

**注:**

> Cassandra实现轻量级事务就是通过IF关键词

## 线性一致性读 ##

[线性一致性](http://docs.datastax.com/en/cassandra/3.0/cassandra/dml/dmlConfigConsistency.html#dmlConfigConsistency__table-write-consistency)允许读取(可能没有commited)当前状态的数据，而不用propose一个新的条件或者更新，如果线性度发现一个未commit的事务，Cassandra会执行read repair作为commit的一部分。
