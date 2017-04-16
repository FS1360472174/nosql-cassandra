package com.fs.service;

import com.fs.dao.DaoUtil;
import com.fs.dao.PersonDao;
import com.fs.dao.PersonDaoImpl;
import com.fs.model.Person;

public class PersonService {
	PersonDao personDao = new PersonDaoImpl();

	public Person getPersonById(String id) {
		personDao = new PersonDaoImpl();
		return personDao.queryById(id);
	}

	public void createSchema() {
		DaoUtil.createKeyspace(Person.DB);
		DaoUtil.createTable(Person.DB, Person.TABLE);

	}

	public void loadData(String id) {
		Person person = new Person();
		person.setId(id);
		person.setName("name" + id);
		person.setDescription("desc" + id);
		personDao.create(person);
	}
}
