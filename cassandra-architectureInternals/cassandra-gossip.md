Gossiper负责确保系统的每个节点都知道其他节点的状态
Gossiper 是一个定时任务。每隔1s执行

1.Gossip to 随机的活的endpoint
2.Gossip 随机的一个不能到达的endpoint
3.如果1gossip到的不是seed。或者存活的节点比seeds数量少，gossip 到一个
随机一个seed节点。

**数据结构**

HeartBeatState
generation and version number.

**Gossip 交换**

发送GossipDigestSynMessage和应答GossipDigestAckMessage


**参考：**

[https://wiki.apache.org/cassandra/ArchitectureInternals](https://wiki.apache.org/cassandra/ArchitectureInternals)
[http://blog.csdn.net/yfkiss/article/details/6943682/](http://blog.csdn.net/yfkiss/article/details/6943682/)
[http://tianya23.blog.51cto.com/1081650/530743](http://tianya23.blog.51cto.com/1081650/530743)