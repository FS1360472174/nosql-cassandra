#为什么需要堆外内存呢#

单有一些大内存对象的时候，JVM进行垃圾回收时需要收集所有的这些对象的内存也。增加了GC压力。因此需要使用堆外内存。

#java 分配堆外内存#

org.apache.cassandra.utils.memory.BufferPool也有相应的实现方法


	 private static ByteBuffer allocate(int size, boolean onHeap)
	    {
	        return onHeap
	               ? ByteBuffer.allocate(size)
	               : ByteBuffer.allocateDirect(size);
	    }


ByteBuffer.allocate: 分配的是JVM内存,这个还是会被JVM回收

ByteBuffer.allocateDirect() 分配OS本地内存，调用的是操作系统的 unsafe.allocateMemory(size);方法分配内存
unsafe.setMemory(base, size, (byte) 0);内存的起始地址和大小存储在java.nio.DirectByteBuffer对象里。只有DirectByteBuffer对象被回收掉，内存才有可能被回收掉。

**内存回收**

当没有更多的堆外内存(-XX:MaxDirectMemorySize)的时候，就会触发一次Full-GC.

#Cassandra 堆外内存管理#
java 提供的堆外内存实现方便，但是有一定缺陷。因为堆外内存是全局的同步锁链表。当多个线程同时检测到堆外内存不够，就会相继的触发full-gc

cassandra中的rowcache是在堆外内存中。

org.apache.cassandra.service.CacheService.initRowCache()

		
		// 调用了OHCProvider方法来实现
		// cache object
        ICache<RowCacheKey, IRowCacheEntry> rc = cacheProvider.create();
        AutoSavingCache<RowCacheKey, IRowCacheEntry> rowCache = new AutoSavingCache<>(rc, CacheType.ROW_CACHE, new RowCacheSerializer());

        int rowCacheKeysToSave = DatabaseDescriptor.getRowCacheKeysToSave();

        rowCache.scheduleSaving(DatabaseDescriptor.getRowCacheSavePeriod(), rowCacheKeysToSave);

org.apache.cassandra.cache.OHCProvider

OHC 就是off-heap-cache


	 public ICache<RowCacheKey, IRowCacheEntry> create()
	    {
	        OHCacheBuilder<RowCacheKey, IRowCacheEntry> builder = OHCacheBuilder.newBuilder();
	        // 1.创建rowcache 内存空间
            builder.capacity(DatabaseDescriptor.getRowCacheSizeInMB() * 1024 * 1024)
	               .keySerializer(KeySerializer.instance)
	               .valueSerializer(ValueSerializer.instance)
	               .throwOOME(true);
	
	        return new OHCacheAdapter(builder.build());
	    }


注1处是调用了ohc-core.jar中OHCacheBuilder来实现堆外内存分配
https://issues.apache.org/jira/browse/CASSANDRA-7438。cassandra目前也将这块的实现抽象出来了，作为一个独立的包，其他的应用也可以调用。

https://github.com/snazy/ohc



# 参考 #

http://blog.csdn.net/xiaofei_hah0000/article/details/52214592
