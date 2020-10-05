/*
 * Copyright 2019-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.init.func;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class EnvironmentUtilsTests {

	private StandardEnvironment environment = new StandardEnvironment();

	@Test
	void simpleInteger() {
		TestPropertyValues.of("server.port=9000").applyTo(environment);
		assertThat(EnvironmentUtils.getProperty(environment, "server.port", Integer.class)).isEqualTo(9000);
	}

	@Test
	void simpleString() {
		TestPropertyValues.of("server.name=foo").applyTo(environment);
		assertThat(EnvironmentUtils.getProperty(environment, "server.name", String.class)).isEqualTo("foo");
	}

	@Test
	void stringArray() {
		TestPropertyValues.of("server.names=foo").applyTo(environment);
		assertThat(EnvironmentUtils.getProperty(environment, "server.names", String[].class)).contains("foo");
	}

	@Test
	void stringList() {
		TestPropertyValues.of("server.names=foo").applyTo(environment);
		@SuppressWarnings("unchecked")
		List<String> list = EnvironmentUtils.getProperty(environment, "server.names", List.class);
		assertThat(list).contains("foo");
	}

	@Test
	void stringSet() {
		TestPropertyValues.of("server.names=foo").applyTo(environment);
		@SuppressWarnings("unchecked")
		Set<String> set = EnvironmentUtils.getProperty(environment, "server.names", Set.class);
		assertThat(set).contains("foo");
	}

	@Test
	void stringCollection() {
		TestPropertyValues.of("server.names=foo,bar").applyTo(environment);
		@SuppressWarnings("unchecked")
		Collection<String> coll = EnvironmentUtils.getProperty(environment, "server.names", Collection.class);
		assertThat(coll).contains("foo", "bar");
	}

}
