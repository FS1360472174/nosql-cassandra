package com.fs.dao;

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

	public static final String db = "test.person";
	
	public PersonDaoImpl(){
		
	}

	@Override
	public void create(Person person) {
		// TODO Auto-generated method stub

	}

	@Override
	public Person queryById(String id) {
		Person person = null;
		Session session = DaoUtil.getSession();
		PreparedStatement ps = session.prepare("select * from " + db + " where id = ? ");
		ps.setConsistencyLevel(ConsistencyLevel.ALL).enableTracing();
		BoundStatement bs = new BoundStatement(ps);
		ResultSet rs = session.execute(bs.bind(id));
		Mapper<Person> mapper = new MappingManager(session).mapper(Person.class);

		Iterator<Row> iter = rs.iterator();
		if (iter.hasNext()) {
			person = mapper.map(rs).all().get(0);
		}
		// Trace from DB
		ExecutionInfo execInfo = rs.getExecutionInfo();
		QueryTrace queryTrace = execInfo.getQueryTrace();
		System.out.println("querTrace:" + queryTrace);
		
		return person;
	}

}
