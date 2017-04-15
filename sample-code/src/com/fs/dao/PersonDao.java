package com.fs.dao;

import com.fs.model.Person;

public interface PersonDao {
	public void create(Person person);

	public Person queryById(String id);
}
