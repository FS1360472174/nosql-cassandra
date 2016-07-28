### **声明** ###
文章发布于[CSDN](http://blog.csdn.net/fs1360472174)
### **cassandra concurrent 具体实现** ###
[cassandra 并发技术](http://blog.csdn.net/FS1360472174/article/details/51809748)文中介绍了java的concurrent实现，这里介绍cassandra如何基于java实现cassandra并发包。

Figure1——cassandra并发实现
![cassandra concurrent实现](http://img.blog.csdn.net/20160708150552212)


1. cassandra各个Stage是通过StageManger来进行管理的,StageManager 有个内部类ExecuteOnlyExecutor。



2. ExecuteOnlyExecutor继承了ThreadPoolExecutor，实现了cassandra的LocalAwareExecutorSerivce接口



3. LocalAwareExecutorService继承了Java的ExecutorService,构建了基本的任务模型。添加了两个自己的方法.
execute方法用于trace跟踪。
	
		public void execute(Runnable command, ExecutorLocals locals);
	    public void maybeExecuteImmediately(Runnable command);

对于Executor中的默认execute方法，和LocalAwareExecutorSerive中的execute方法都是new 一个task,然后将task添加到queue中。而maybeExecuteImmedicatly方法则是判断下是否有正在执行的task或者work，如果没有则直接执行，而不添加到队列中。

		public void maybeExecuteImmediately(Runnable command)
		{
			//comment1
		    FutureTask<?> ft = newTaskFor(command, null);
		    if (!takeWorkPermit(false))
		    {
		        addTask(ft);
		    }
		    else
		    {
		        try
		        {
		            ft.run();
		        }
		        finally
		        {
		            returnWorkPermit();
		            maybeSchedule();
		        }
		    }
		}


4. AbstractLocalAwareExecutorService实现LocalAwareExecutorSerive接口，提供了executor的实现以及ExecutorServie接口中的关于生命周期管理的方法实现，如submit，shoudown等方法。添加了addTask，和任务完成的方法onCompletion。



5.  SEPExecutor实现了LocalAwareExecutorService类,提供了addTask，onCompletion,maybeExecuteImmediately等方法的实现。同时负责队列的管理

6. SharedExecutorPool，线程池管理，用来管理Executor



### **cassandra write ** ###
cassandra写操作涉及到MutationStage,FlushWriter,MemtablePostFlusher,ReplicateOnWriteStage

**MutationStage**

Figure2 cassandra mutation change(coordinator)
![cassandra write path](http://img.blog.csdn.net/20160708151403127)
cassandra mutation时序图如上图所示。前面几个都是线程调用和request的"翻译"重点是最后一个类的执行StorageProxy.在#comment1处，cassandra对batch change 和涉及到view更新 与单条的insert操作进行了区分。

 - **Single**
Coordinator:将request同时发给所有replicate节点
Replicate:
		1.写数据到commitlog 
		2. 写数据到MemTable
		3. 如果写操作是个delete操作，在commitlog和MemTable中添加墓碑tombstone
		4. 如果使用了row caching,需要失效这行的缓存
		5. 发送应答request到coordinator
 
 - **View/Batch**
	View是和Table绑定在一起的，所以要确保两者是一起更新的。cassandra通过batch log 来实现。无论write consistency level 是多少，batch log 要确保change写入到了quorum份replicate.

	Coordinator:创建batch log,确保quorum份replicate node写入change,客户端的响应仍然是按照write consistency level;将request同时发给所有replicate 节点。
Replicate:完整调用栈见Figure3
	1. 获得partition 的锁，确保batch/view 的write request是串行化(BatchManger.store#comment1 处)
	2. 如果是视图，则需要读取partition 数据，生成物化视图的增量变化
	3. 写 commit log
	4. 生成batch log
	5. 存储batch log
	6. 发送batch的second write/物化视图的更新到相应的replicate node。因为batch/视图更新 等多条记录可能不在同一个replicate上
	7. 写MemTable
	8. 其他record的replicate会写更新，然后发送response到first record replicate node 上
		//BatchManager.store()
		
			public static void store(Batch batch, boolean durableWrites) {
			RowUpdateBuilder builder = new RowUpdateBuilder(SystemKeyspace.Batches, batch.creationTime, batch.id)
					.clustering().add("version", MessagingService.current_version);
	
			for (ByteBuffer mutation : batch.encodedMutations)
				builder.addListEntry("mutations", mutation);
	
			for (Mutation mutation : batch.decodedMutations) {
				try (DataOutputBuffer buffer = new DataOutputBuffer()) {
					//comment1 串行化多个mutation改变
					Mutation.serializer.serialize(mutation, buffer, MessagingService.current_version);
					builder.addListEntry("mutations", buffer.buffer());
				} catch (IOException e) {
					// shouldn't happen
					throw new AssertionError(e);
				}
			}
	
			builder.build().apply(durableWrites);
			}
Figure3 cassandra mutation change(replicate)
![这里写图片描述](http://img.blog.csdn.net/20160708172346100)

		
**FlushWriter&&MemtablePostFlusher**

FlushWriter Stage 就是将数据从MemTable flush到SSTable中。有三种事件会导致发生
1. memtable 超过了设定的大小
2. nodetool flush
3. commit log 超过了设定的大小



**ReplicateOnWriteStage**

### 参考 ###
[https://wiki.apache.org/cassandra/WritePathForUsers](https://wiki.apache.org/cassandra/WritePathForUsers)

[http://www.mikeperham.com/2010/03/13/cassandra-internals-writing/](http://www.mikeperham.com/2010/03/13/cassandra-internals-writing/)

[https://wiki.apache.org/cassandra/ArchitectureInternals](https://wiki.apache.org/cassandra/ArchitectureInternals)
