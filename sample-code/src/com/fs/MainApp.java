package com.fs;

import com.fs.service.CassandraService;
import com.fs.service.PersonService;

public class MainApp {

	public static void main(String[] args) {
//		CassandraService cassandraService = new CassandraService();
//		cassandraService.startCassandra();
		PersonService personService = new PersonService();
		String id = "1";
		personService.getPersonById(id);
	}

}
