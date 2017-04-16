package com.fs.dao;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;

public class DaoUtil {
	private static Cluster cluster;
	private static Session session;

	public static final String host = "localhost";

	static {
		// cluster = Cluster.builder().addContactPoint(host)
		// .withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc("dc1").build()).build();
		cluster = Cluster.builder().addContactPoint(host).build();
		session = cluster.connect();
	}

	public static Session getSession() {
		return session;
	}

	public static void close() {
		session.close();
		cluster.close();
	}

	public static void createKeyspace(String keyspace) {
		session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace
				 + " WITH replication={'class': 'SimpleStrategy', 'replication_factor':1};");
	}

	public static void createTable(String keyspace, String table) {
		session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + "." + table +
				" (id text, name text, description text, PRIMARY KEY(id));");
	}
}
