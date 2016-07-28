---

title: cassandra bootstrap 过程

---

1.需要等待集群中所有的node意识到正在进行bootstrapping 的节点。然后再去做另外一个
bootstrap.

新加入的节点首先要收集要加载的信息，然后等待其他节点将相应信息发送过来


[https://wiki.apache.org/cassandra/Operations](https://wiki.apache.org/cassandra/Operations)


StreamTransferTask.addTransferFiles(Collection<SSTableStreamingSections>)
