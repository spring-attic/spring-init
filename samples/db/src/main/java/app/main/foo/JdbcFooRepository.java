/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.main.foo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * @author Dave Syer
 *
 */
@Repository
public class JdbcFooRepository implements FooRepository {

	private final NamedParameterJdbcTemplate template;

	public JdbcFooRepository(NamedParameterJdbcTemplate template) {
		this.template = template;
	}

	@Override
	public Foo find(long id) {
		try {
			return template.queryForObject("select id, value from foos where id=:id",
					Collections.singletonMap("id", id), new FooRowMapper());
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public void save(Foo foo) {
		if (foo.getId() == null) {
			KeyHolder holder = new GeneratedKeyHolder();
			template.update("INSERT into foos (value) values (:value)",
					new BeanPropertySqlParameterSource(foo), holder);
			foo.setId(holder.getKey().longValue());
		}
		else {
			template.update("UPDATE foos set value=:value where id=:id",
					new BeanPropertySqlParameterSource(foo));
		}
	}

	private static class FooRowMapper implements RowMapper<Foo> {

		@Override
		public Foo mapRow(ResultSet rs, int rowNum) throws SQLException {
			Foo foo = new Foo(rs.getString(2));
			foo.setId(rs.getLong(1));
			return foo;
		}

	}

}
