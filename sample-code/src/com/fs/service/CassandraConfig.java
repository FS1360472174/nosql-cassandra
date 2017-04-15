package com.fs.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.ConfigurationLoader;
import org.apache.cassandra.config.ParameterizedClass;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.locator.SimpleSeedProvider;

public class CassandraConfig implements ConfigurationLoader {

	static final String COMMITLOG_FOLDER = "build/cassandra/commitlog";
	static final String HINT_FOLDER = "build/cassandra/hint";
	static final String SAVED_CACHE = "build/cassandra/saved_caches";
	static final String[] DATA_FOLDER = new String[] { "build/cassandra/data" };

	@Override
	public Config loadConfig() throws ConfigurationException {
		Config config = new Config();
		config.data_file_directories = DATA_FOLDER;
		config.commitlog_sync = Config.CommitLogSync.periodic;
		config.commitlog_sync_period_in_ms = 6000;
		config.commitlog_directory = COMMITLOG_FOLDER;
		config.partitioner = Murmur3Partitioner.class.getName();
		config.endpoint_snitch = "SimpleSnitch";
		config.hints_directory = HINT_FOLDER;
		config.saved_caches_directory = SAVED_CACHE;
		Map<String, String> seed = new HashMap<String, String>();
		seed.put("seeds", "localhost");
		config.seed_provider = new ParameterizedClass("org.apache.cassandra.locator.SimpleSeedProvider", seed);
		config.listen_address = "localhost";
		config.rpc_port = 9160;
		config.native_transport_port = 9042;
		config.storage_port = 7010;
		config.rpc_address = "localhost";
		config.start_native_transport = true;

		return config;
	}

}
