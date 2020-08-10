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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.squareup.javapoet.JavaFile;

public class ClosedWorldProcessorTests {

	@BeforeAll
	static public void init() {
		InitializerApplication.closedWorld = true;
	}

	@AfterAll
	static public void close() {
		InitializerApplication.closedWorld = false;
	}

	@Test
	public void scanSubPackage() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("scan.sub"));
		assertThat(files).hasSizeGreaterThan(1);
		// System.err.println(files);
		assertThat(StringUtils.countOccurrencesOf(files.toString(), "defer(")).isGreaterThan(1);
		assertThat(files.toString()).contains("new SampleConfigurationInitializer().initialize(context)");
		assertThat(files.toString()).contains("context.getBean(SampleConfiguration.class).foo()");
		assertThat(files.toString()).contains("AutoConfigurationPackages.register(context, \"app.scan.sub\")");
		assertThat(files.toString())
		.contains("registrars.defer(new PropertyPlaceholderAutoConfigurationInitializer())");
		assertThat(files.toString())
		.contains("EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY");
	}

	@Test
	public void managementContext() {
		Set<JavaFile> files = new InitializerClassProcessor().process(ManagementContextAutoConfiguration.class);
		// System.err.println(files);
		assertThat(files).hasSize(7);
		assertThat(files.toString()).contains("new ManagementContextAutoConfiguration_SameManagementContextConfigurationInitializer().initialize(context)");
	}


	private Class<?> app(String pkg) {
		return ClassUtils.resolveClassName("app." + pkg + ".SampleApplication", null);
	}

}
