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
import java.io.IOException;
import java.util.Set;

import org.springframework.util.ClassUtils;

import com.squareup.javapoet.JavaFile;

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
			} else {
				// Assume it's a package:
				files = processor.process(start);
			}
			for (JavaFile file : files) {
				try {
					file.writeTo(dir);
				} catch (IOException e) {
					throw new IllegalStateException("Cannot write in: " + dir);
				}
			}
		} finally {
			closedWorld = false;
		}
	}

}
