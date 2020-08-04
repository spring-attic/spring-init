/*
 * Copyright 2020-2020 the original author or authors.
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
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.squareup.javapoet.JavaFile;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.DefaultPropertiesPersister;

/**
 * @author Dave Syer
 *
 */
public class InitializerApplication {

	static boolean closedWorld = false;

	public static void main(String[] args) {
		String start = args[0];
		File dir = new File(args[1]);
		dir.mkdirs();
		InitializerClassProcessor processor = new InitializerClassProcessor();
		Set<JavaFile> files;
		if (!System.getProperty("spring.init.closed-world", "false").equals("false")) {
			closedWorld = true;
		}
		try {
			if (ClassUtils.isPresent(start, null)) {
				// It's a class:
				Class<?> type = ClassUtils.resolveClassName(start, null);
				files = processor.process(type);
			}
			else {
				// Assume it's a package:
				files = processor.process(start);
			}
			for (JavaFile file : files) {
				try {
					file.writeTo(dir);
				}
				catch (IOException e) {
					throw new IllegalStateException("Cannot write in: " + dir);
				}
			}
			if (!System.getProperty("spring.init.build-time-location", "").equals("")) {
				String location = System.getProperty("spring.init.build-time-location");
				Set<String> buildTimes = processor.getBuildTimes();
				updateNativeImageProperties(buildTimes, location);
			}
		}
		finally {
			closedWorld = false;
		}
	}

	static void updateNativeImageProperties(Set<String> buildTimes, String location) {

		if (!buildTimes.isEmpty()) {

			try {
				File meta = new File(location);
				meta.mkdirs();
				File nat = new File(meta, "native-image.properties");
				Properties props = nat.exists() ? PropertiesLoaderUtils.loadProperties(new FileSystemResource(nat))
						: new Properties();
				String natargs = props.getProperty("args", props.getProperty("Args", ""));
				Set<String> toAppend = new HashSet<>();
				for (String type : buildTimes) {
					if (!natargs.contains(type)) {
						toAppend.add(type);
					}
				}
				if (toAppend.isEmpty()) {
					return;
				}
				StringBuilder builder = new StringBuilder(natargs);
				if (builder.length() > 0) {
					builder.append(" \\\n");
				}
				builder.append("--initialize-at-build-time=");
				int count = 0;
				for (String type : toAppend) {
					builder.append(type);
					if (++count < toAppend.size()) {
						builder.append(",");
					}
				}
				if (props.containsKey("args")) {
					props.setProperty("args", builder.toString());
				}
				else {
					props.setProperty("Args", builder.toString());
				}
				new DefaultPropertiesPersister().store(props, new FileOutputStream(nat), "Native Image Properties");
			}
			catch (IOException e) {
				throw new IllegalStateException("Could not create native-image.properties", e);
			}

		}

	}

}
