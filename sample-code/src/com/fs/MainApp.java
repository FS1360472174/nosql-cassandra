package com.fs;

import com.fs.model.Person;
import com.fs.service.CassandraService;
import com.fs.service.PersonService;

public class MainApp {

	public static void main(String[] args) {
		CassandraService cassandraService = new CassandraService();
		cassandraService.startCassandra();
		testPerson();
	}

	public static void testPerson() {
		PersonService personService = new PersonService();
		personService.createSchema();
		String id = "1";
		personService.loadData(id);
		Person person = personService.getPersonById(id);
		System.out.println("person:id" + person.getId());
	}

}
