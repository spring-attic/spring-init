/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.init.tools;

import java.util.Properties;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomBindersTests {

	@BeforeAll
	static public void init() {
		System.setProperty("spring.init.custom-binders", "true");
	}

	@AfterAll
	static public void close() {
		System.clearProperty("spring.init.custom-binders");
	}

	@Test
	public void messageProperties() {
		JavaFile file = generate("spring.messages.basename=messages/messages");
		// System.err.println(file);
		assertThat(file.toString())
				.contains("import org.springframework.boot.autoconfigure.context.MessageSourceProperties;");
		assertThat(file.toString())
				.contains("MessageSourceProperties messageSourceProperties(MessageSourceProperties bean,");
	}

	@Test
	public void serverProperties() {
		JavaFile file = generate("server.port=${PORT:8080}");
		// System.err.println(file);
		assertThat(file.toString()).contains("import org.springframework.boot.autoconfigure.web.ServerProperties;");
		assertThat(file.toString())
				.contains("bean.setPort(EnvironmentUtils.getProperty(environment, \"server.port\", Integer.class");
		assertThat(file.toString()).contains("ServerProperties serverProperties(ServerProperties bean,");
	}

	@Test
	public void dataSourceProperties() {
		JavaFile file = generate("spring.datasource.name=foo");
		// System.err.println(file);
		assertThat(file.toString())
				.contains("import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;");
		assertThat(file.toString()).contains("bean.setName(");
		assertThat(file.toString()).contains("DataSourceProperties dataSourceProperties(DataSourceProperties bean,");
	}

	@Test
	// @Disabled
	public void dataSourceSchema() {
		JavaFile file = generate("spring.datasource.schema=classpath:/db/schema.sql");
		// System.err.println(file);
		assertThat(file.toString()).contains("@SuppressWarnings(\"unchecked\")");
		assertThat(file.toString())
				.contains("import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;");
		assertThat(file.toString()).contains("bean.setSchema(");
		assertThat(file.toString()).contains("DataSourceProperties dataSourceProperties(DataSourceProperties bean,");
	}

	private JavaFile generate(String... props) {
		TypeSpec.Builder type = TypeSpec.classBuilder("Generated");
		type.addMethods(new CustomBinderBuilder().getBinders(props(props)));
		JavaFile file = JavaFile.builder("app.main", type.build()).build();
		return file;
	}

	private Properties props(String... strings) {
		Properties props = new Properties();
		for (String string : strings) {
			String[] split = StringUtils.split(string, "=");
			String key = split[0];
			props.setProperty(key, split.length > 1 ? split[1] : "");
		}
		return props;
	}

}
