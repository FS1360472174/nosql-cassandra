#摘要#
CQL相比较于SQL有很多的限制，原因在于cassandra是为大数据存储设计的，而它的部署模式又都是基于分区方式的，不像mongo还有复制集这个小规模的数据库集群设计，当数据量大的时候再进行分片。为了提供检索效率，所以在CQL语法上做了限制，避免低效的查询语句。cassandra的数据是根据partition key做hash计算后分布到各个节点的，扫描各个节点的效率是很低的。所以cassandra query检索的一个基本原则就是尽可能的查找少的节点。

#CQL#

##概览##

关系型数据是行的集合，Cassandra是分区的集合，如果没有clustering key的话，每个分区就是单行，一个分区包含多行的话就叫做宽行(wide-row)。cassandra 根据partition key的hash value来决定数据存储在哪个节点，这个相当于是hash索引，所以不能进行范围查询。然后在每个分区根据clustering key来排序，
这个不是基于hash的，所以可以根据范围来查询。以下表为例，node为partition key,(date,number)是clustering key.
 
	CREATE KEYSPACE test WITH REPLICATION = {
	    'class': 'SimpleStrategy',
	    'replication_factor': 1
    };

    CREATE TABLE log(
    	node text,
	    date text,
	    name text,
	    number int,
    	Primary Key(node,date,number)
    );

在关系型数据库中数据是这么存储的



| node | date | number | name|
| ------------- |:-------------:| -----:|-----:|
| n1 | feb | 1 | name1|
| n1 | feb | 2 | name2|
| n2 | feb | 1 | name3|
| n2 | feb | 2 | name4|

而在cassandra则是

partition1

    {
		date:feb {number:1 {name:name1}} 
				 {number:2 {name:name2}}
	}

partition2 

  	{
		date:feb {number:1 {name:name3}} 
				 {number:2 {name:name5}}
	}

> cassandra 的列不固定，所以列名也要存。

##范围查询##

  

- IN操作

	cassandra 2.2 以前IN只能用于partition key的最后一列，2.2之后改进了可以用于partition key的任意一列了。但是要注意IN操作的效率比较低，在摘要中已经说过了要尽可能的减少查询的节点，in操作会查询多个节点，如果设置了replicat factor为3的话，查询的节点数又会三倍增加。这样coordinate 节点的查询压力就会增加，需要保存各个节点的查询结果，就有可能会导致GC暂停，堆内存增加。
	
	cassandra 3.0之后的IN支持在clustering key的列，不过如果是单列，必须要指定之前clustering key的值。（=或者IN）

- 范围查询>,<

**partition key**

Cassandra是基于partition key的hash分步数据的，所以不支持范围查询。
允许在partition key的字段上面利用token函数来进行范围查询。

	SELECT * FROM log WHERE token(node) > token('1') 

> 注：分区器ByteOrderedPartitioner是有序分布数据的，所以理论上应该可以支持范围查询，但是使用这种分区器容易导致数据分布的不平衡，所以一搬不推荐使用

**clustering key**

单列的话，范围查询只能用于最后一列。前面的列必须要给定

	SELECT *FROM log WHERE node = '1' AND date >='date1'

	SELECT *FROM log WHERE node = '1' AND date = 'date' AND number >=1

无效的就是这种

	SELECT *FROM log where node = '1' AND number >= 1

# 总结 #

1. partition key是基于hash，不支持大于，小于范围查询，IN操作的是可以支持的。

2. clustering key是排序的，支持IN,大于小于查询。它相当于一个联合索引，所以要想让联合索引生效，得保证前面字段都有。

3. 当没有给定partition key,而给定clustering key，需要扫描所有节点，然后进行过滤。所以执行效率可能比较低。所以Cassandra 要求使用ALLOW FILTERING。当过滤到的数据占查询的数据比例比较高的时候，还比较有效。
#问题#

- 问题1:where 语法中支不支持or

	不支持，or 在SQL中一般是用来减少发到DB的请求。其中每个条件都构成了单个查询。Cassandra要求尽可能的减少请求节点，要求在每次查询时指定partition key，显然对于or这样多字段的条件，很难分析语境。

- 问题2：cassandra支不支持模糊查询

	不支持LIKE操作，cassandra3.4以后可以使用，但是有很多限制。可以看下这篇文章介绍http://www.tsoft.se/wp/2016/08/12/sql-like-operation-in-cassandra-is-possible-in-v3-4/
- 问题3: 一张表支持上千个字段吗
   
    支持，对字段的数目没有限制，只有对paritition的数目有20亿的限制。所以列越多的话，相应的能够存储的行数就少了。
	


# 参考 #
https://lostechies.com/ryansvihla/2014/09/22/cassandra-query-patterns-not-using-the-in-query-for-multiple-partitions/

https://wiki.apache.org/cassandra/CassandraLimitations


http://www.tsoft.se/wp/2016/08/12/sql-like-operation-in-cassandra-is-possible-in-v3-4/

https://github.com/xedin/sasi