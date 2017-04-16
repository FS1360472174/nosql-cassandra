package com.fs.model;

import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "test", name = "person")
public class Person {
	public static final String DB = "test";
	public static final String TABLE = "person";

	private String id;
	private String name;
	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
