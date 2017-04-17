package com.fs.dao;

import java.text.SimpleDateFormat;
import java.util.Iterator;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ExecutionInfo;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.QueryTrace;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fs.model.Person;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

public class PersonDaoImpl implements PersonDao {
	Session session = DaoUtil.getSession();

	public PersonDaoImpl() {

	}

	@Override
	public void create(Person person) {
		System.out.println("insert person:" + person);
		PreparedStatement ps = session
				.prepare("insert into " + Person.DB + "." + Person.TABLE + "(id, name, description)values(?,?,?)");
		BoundStatement bs = new BoundStatement(ps);
		session.execute(bs.bind(person.getId(), person.getName(), person.getDescription()));
	}

	@Override
	public Person queryById(String id) {
		System.out.println("query person by id:" + id);
		Person person = null;
		PreparedStatement ps = session.prepare("select * from " + Person.DB + "." + Person.TABLE + " where id = ? ");
		ps.setConsistencyLevel(ConsistencyLevel.ALL).enableTracing();
		BoundStatement bs = new BoundStatement(ps);
		ResultSet rs = session.execute(bs.bind(id));
		Mapper<Person> mapper = new MappingManager(session).mapper(Person.class);

		Iterator<Row> iter = rs.iterator();
		if (iter.hasNext()) {
			person = mapper.map(rs).all().get(0);
		}
		// Trace from DB
		QueryTrace queryTrace = rs.getExecutionInfo().getQueryTrace();

		System.out.printf("Trace id: %s\n\n", queryTrace.getTraceId());
		System.out.printf("%-42s | %-12s | %-10s \n", "activity", "timestamp", "source");

		System.out.println("-------------------------------------------");
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

		for (QueryTrace.Event event : queryTrace.getEvents()) {
			System.out.printf("%42s | %12s | %10s\n", event.getDescription(), dateFormat.format((event.getTimestamp())),
					event.getSource());
		}

		return person;
	}

}
