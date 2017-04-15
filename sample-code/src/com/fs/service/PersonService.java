package com.fs.service;

import com.fs.dao.PersonDao;
import com.fs.dao.PersonDaoImpl;
import com.fs.model.Person;

public class PersonService {
	public Person getPersonById(String id) {
		PersonDao personDao = new PersonDaoImpl();
		return personDao.queryById(id);
	}
}
