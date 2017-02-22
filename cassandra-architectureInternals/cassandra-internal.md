**写在前面**

cassandra3.x官方文档的非官方翻译。翻译内容水平全依赖本人英文水平和对cassandra的理解。所以强烈建议阅读英文版[cassandra 3.x 官方文档](http://docs.datastax.com/en/cassandra/3.0/)。此文档一半是翻译，一半是个人对cassandra的认知。尽量将我的理解通过引用的方式标注，以示区别。另外文档翻译是项长期并有挑战的工作，如果你愿意加入[cassandra git book](https://www.gitbook.com/book/fs1360472174/cassandra-document/details),可以发信给我。当然你也可以加入我们的QQ群,104822562。一起学习探讨cassandra.

#数据存储引擎#

Cassandra使用类似于LSM树([Log-Structured Merge Tree](https://en.wikipedia.org/wiki/Log-structured_merge-tree)),不像传统的关系型数据库使用B-Tree树。Cassandra避免在写之前还要读。在写之前读，尤其是在大型分布式系统，会造成读性能的很大延迟和其他问题。例如，两个client在同一时间读;其中一个重写了行，进行了A更新。而另外一个客户端重写了行进行了B更新，移除了A更新。这种竞态条件会导致不明确的查询结果-谁的更新是对的？

为了避免Cassandra中的大部分写使用写之前读，存储引擎在内存中将inserts和update分组，并且不时的，以追加的方式将数据顺序的写到磁盘中。一旦写入到了磁盘，数据不可更改,不能被覆写。读数据时需要组合不可更改的顺序写入的数据去发现正确的查询结果。可以使用[轻量级事务](http://docs.datastax.com/en/cassandra/3.0/cassandra/dml/dmlLtwtTransactions.html)在写入之前检查数据的状态。然而，这个功能建议限制使用。

一个日志结构的引擎避免覆写、使用顺序I/O来更新数据对于写入SSD和HDD是非常有效的，随机写磁盘涉及到更高的查询操作相比较顺序写。查询的代价可能非常大。因为Cassandra顺序写不可改变的文件，因此避免了[写入放大](https://zh.wikipedia.org/wiki/%E5%86%99%E5%85%A5%E6%94%BE%E5%A4%A7)和磁盘故障，数据库存储不昂贵，SSDs尤其收益更大，对于大多数数据库，在SSDs写入放大是一个问题。

**注:**
>1.固态硬盘VS机械硬盘
>
> 固态硬盘和机械硬盘的区别在于传统的机械硬盘使用磁介质来保存数据，数据读写的时候需要转动磁盘，因此顺序写比随机写的效率更高。而SSD使用闪存作为存储介质，不像机械硬盘那样有活动的机械部件。因此SSD的随机写和顺序写区别不大。
> 
> 2.写入放大
> 写入放大是一个在闪存和SSD中会发生的，不会发生在机械硬盘上，所谓写入放大就是写入的物理数据量是写入数据量的多倍。这个现象会发生的原因就是闪存在重新写入数据前必须先擦除