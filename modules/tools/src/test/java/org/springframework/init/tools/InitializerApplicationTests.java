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
package org.springframework.init.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.init.tools.cond.ConditionalApplication;
import org.springframework.init.tools.manual.ManualApplication;
import org.springframework.init.tools.manual.ManualApplicationInitializer;
import org.springframework.init.tools.manual.SampleConfigurationInitializer;
import org.springframework.util.ClassUtils;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
class InitializerApplicationTests {

	@Test
	void filesCreated() throws Exception {
		File file = new File("target/test");
		if (file.exists()) {
			file.delete();
		}
		InitializerApplication.main(new String[] { ConditionalApplication.class.getName(), "target/test" });
		assertThat(new File(file,
				ClassUtils.convertClassNameToResourcePath(ConditionalApplication.class.getName() + "Initializer")
						+ ".java")).exists();
	}

	@Test
	void filesNotCreated() throws Exception {
		File file = new File("target/test");
		if (file.exists()) {
			file.delete();
		}
		InitializerApplication.main(new String[] { ManualApplication.class.getName(), "target/test" });
		assertThat(new File(file,
				ClassUtils.convertClassNameToResourcePath(ManualApplicationInitializer.class.getName()) + ".java"))
						.doesNotExist();
		assertThat(new File(file,
				ClassUtils.convertClassNameToResourcePath(SampleConfigurationInitializer.class.getName()) + ".java"))
						.doesNotExist();
	}

	@Test
	void propertiesFileCreated() throws Exception {
		File file = new File("target/native-image/native-image.properties");
		file.getParentFile().mkdirs();
		if (file.exists()) {
			file.delete();
		}
		InitializerApplication.updateNativeImageProperties(
				Collections.singleton("app.main.SampleConfigurationInitializer"), "target/native-image");
		Properties props = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		assertThat(props).containsKey("Args");
		assertThat(props.getProperty("Args")).startsWith("--initialize-at-build-time");
	}

	@Test
	void propertiesFileNotAppended() throws Exception {
		File file = new File("target/native-image/native-image.properties");
		file.getParentFile().mkdirs();
		if (file.exists()) {
			file.delete();
		}
		InitializerApplication.updateNativeImageProperties(
				Collections.singleton("app.main.SampleConfigurationInitializer"), "target/native-image");
		InitializerApplication.updateNativeImageProperties(
				Collections.singleton("app.main.SampleConfigurationInitializer"), "target/native-image");
		Properties props = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		assertThat(props).containsKey("Args");
		String args = props.getProperty("Args");
		assertThat(args).startsWith("--initialize-at-build-time");
		assertThat(args).satisfies(
				value -> assertThat(StringUtils.countOccurrencesOf(value, "--initialize-at-build-time")).isEqualTo(1));
	}

	@Test
	void propertiesFileAppended() throws Exception {
		File file = new File("target/native-image/native-image.properties");
		file.getParentFile().mkdirs();
		if (file.exists()) {
			file.delete();
		}
		Properties props = new Properties();
		props.setProperty("Args", "--initialize-at-build-time=foo.Bar");
		new DefaultPropertiesPersister().store(props, new FileOutputStream(file), "Test");
		InitializerApplication.updateNativeImageProperties(
				Collections.singleton("app.main.SampleConfigurationInitializer"), "target/native-image");
		props = PropertiesLoaderUtils.loadProperties(new FileSystemResource(file));
		// System.err.println(props);
		assertThat(props).containsKey("Args");
		String args = props.getProperty("Args");
		assertThat(args).startsWith("--initialize-at-build-time");
		// assertThat(args).contains("foo.Bar \\\n");
		assertThat(args).contains("foo.Bar --");
		assertThat(args).satisfies(
				value -> assertThat(StringUtils.countOccurrencesOf(value, "--initialize-at-build-time")).isEqualTo(2));
	}

}
