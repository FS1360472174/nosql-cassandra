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
}
