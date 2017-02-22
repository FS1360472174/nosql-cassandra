cassandra 3.x官方文档(3)---gossip通信协议及故障检测与恢复

**写在前面**
cassandra3.x官方文档的非官方翻译。翻译内容水平全依赖本人英文水平和对cassandra的理解。所以强烈建议阅读英文版[cassandra 3.x 官方文档](http://docs.datastax.com/en/cassandra/3.0/)。此文档一半是翻译，一半是个人对cassandra的认知。尽量将我的理解通过引用的方式标注，以示区别。另外文档翻译是项长期并有挑战的工作，如果你愿意加入[cassandra git book](https://www.gitbook.com/book/fs1360472174/cassandra-document/details),可以发信给我。当然你也可以加入我们的QQ群,104822562。一起学习探讨cassandra.

失败检测是一种为本地决策提供信息的方法，从gossip的状态和历史获取信息，判断系统中的一个节点是否down了或者已经恢复了。Cassandra 使用这个信息避免将客户端的请求路由到任何时候有可能不可到达的节点。(cassandra 同样能够通过[http://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archSnitchesAbout.html](http://docs.datastax.com/en/cassandra/3.0/cassandra/architecture/archSnitchesAbout.html "dynamic snitch"))避免将客户端请求路由到那些存活的但是性能比较差的节点上。

gossip过程能够跟踪其他节点的状态，通过直接(直接与某个节点gossip)或非直接(通过二手，三手等)方式。相比于一个固定的阈值来标记一个节点为fail，Cassandra 采用一个自然增长的检测机制来计算每个节点的阈值，考虑到了网络、负载、历史状况等因素。当进行gossip交换时，每个节点维护了一个其他节点gossip信息到达的滑动窗口时间。可以通过配置[http://docs.datastax.com/en/cassandra/3.0/cassandra/configuration/configCassandra_yaml.html#configCassandra_yaml__phi_convict_threshold](http://docs.datastax.com/en/cassandra/3.0/cassandra/configuration/configCassandra_yaml.html#configCassandra_yaml__phi_convict_threshold "phi_convict_threshold")属性来调节失败检测的敏感性。值越低，一个没有应答的节点更有可能被标记为down,值越高，短暂的失败更低可能的被标记为失败。大部分情况下，默认值就可以了。但是在Amazon EC2上需要增加到10或者12.(因为常常会遇到网络拥堵)，在不稳定的网络环境中(比如EC2)，提高值到10或者12可以帮助避免错误的失败检测。不建议使用高于12，或者低于5的值。

节点失败可能有各种各样的原因导致，比如硬件失败，网络电力供应中断。节点中断经常是短暂的但是有可能持续很长时间的。因为一个节点中断很少意味着永久离开集群，不会自动从集群ring中移除。其他的节点会间断性的尝试重新和失败的节点重新建立联系，看它们是否已经回归。想要永久的改变集群节点的成员关系，需要管理员通过notetool准确的将节点添加进来或者移除出集群。

当一个节点经过down到重新回归的，可能会丢失掉它需要维护的副本数据。
[http://docs.datastax.com/en/cassandra/3.0/cassandra/operations/opsRepairNodesTOC.html](http://docs.datastax.com/en/cassandra/3.0/cassandra/operations/opsRepairNodesTOC.html "Repair mechanisms")可以帮助恢复这些数据，比如hinted handoffs以及手动repair.节点down掉的时间决定了通过哪种机制来保持数据的一致性