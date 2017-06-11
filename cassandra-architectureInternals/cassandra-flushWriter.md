memtable 是基于表来存储的，所以force flush提供的参数就是<keyspace> <tables>两个参数。 
1. nodetool  可以强制flush

org.apache.cassandra.tools.nodetool.Flush


org.apache.cassandra.service.StorageService

    public void forceKeyspaceFlush(String keyspaceName, String... tableNames) throws IOException
    {
        for (ColumnFamilyStore cfStore : getValidColumnFamilies(true, false, keyspaceName, tableNames))
        {
            logger.debug("Forcing flush on keyspace {}, CF {}", keyspaceName, cfStore.name);
			//由column family来执行对应的表的flush
            cfStore.forceBlockingFlush();
        }
    }


org.apache.cassandra.db.ColumnFamily


	public void forceBlockingFlush()
    {
        FBUtilities.waitOnFuture(forceFlush());
    }

核心处理代码就是forceFlush()方法。

	public ListenableFuture<?> forceFlush(ReplayPosition flushIfDirtyBefore)
    {
        //1.需要处理的memtable data
        synchronized (data)
        {
            
			// memtable 的flush过程需要同时flush secondary index
            // during index build, 2ary index memtables can be dirty even if parent is not.  if so,
            // we want to flush the 2ary index ones too.
            boolean clean = true;
            for (ColumnFamilyStore cfs : concatWithIndexes())
                clean &= cfs.data.getView().getCurrentMemtable().isCleanAfter(flushIfDirtyBefore);

            if (clean)
            {
                // We could have a memtable for this column family that is being
                // flushed. Make sure the future returned wait for that so callers can
                // assume that any data inserted prior to the call are fully flushed
                // when the future returns (see #5241).
                ListenableFutureTask<?> task = ListenableFutureTask.create(new Runnable()
                {
                    public void run()
                    {
                        logger.trace("forceFlush requested but everything is clean in {}", name);
                    }
                }, null);
                postFlushExecutor.execute(task);
                return task;
            }

            return switchMemtable();
        }
    }

1. data
就是Memtables，以及在磁盘上的SSTables。需要使用synchronize来确保隔离性。在CF类初始化的时候会进行加载

   public ColumnFamilyStore(Keyspace keyspace,
                              String columnFamilyName,
                              int generation,
                              CFMetaData metadata,
                              Directories directories,
                              boolean loadSSTables,
                              boolean registerBookkeeping)
	{ 

        data = new Tracker(this, loadSSTables);

        if (data.loadsstables)
        {
            Directories.SSTableLister sstableFiles = directories.sstableLister(Directories.OnTxnErr.IGNORE).skipTemporary(true);
            Collection<SSTableReader> sstables = SSTableReader.openAll(sstableFiles.list().entrySet(), metadata);
            data.addInitialSSTables(sstables);
        } 
    }
