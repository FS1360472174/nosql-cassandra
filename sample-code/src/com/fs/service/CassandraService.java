package com.fs.service;

import java.io.IOException;

import org.apache.cassandra.service.EmbeddedCassandraService;

public class CassandraService {

	public void startCassandra() {

		EmbeddedCassandraService cassandra = new EmbeddedCassandraService();
		System.setProperty("cassandra.config.loader","com.fs.service.CassandraConfig");
		try {
			cassandra.start();
		} catch (IOException e) {
			System.out.println("start cassandra failed" + e);
		}
	}
	
}
