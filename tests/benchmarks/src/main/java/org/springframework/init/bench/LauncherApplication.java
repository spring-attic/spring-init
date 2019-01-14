/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.init.bench;

import java.io.Closeable;
import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class LauncherApplication implements Runnable, Closeable {

	private ConfigurableApplicationContext context;
	private Class<?> main;
	private String[] args = new String[0];

	public LauncherApplication(Class<?> main) {
		this.main = main;
	}

	public static void run(Class<?> main, String[] args) throws Exception {
		try (LauncherApplication app = new LauncherApplication(main)) {
			app.setArgs(args);
			app.run();
		}
	}

	private void setArgs(String[] args) {
		this.args = args;
	}

	public ConfigurableApplicationContext getContext() {
		return this.context;
	}

	@Override
	public void close() throws IOException {
		if (this.context != null && this.context.isActive()) {
			this.context.close();
		}
	}

	@Override
	public void run() {
		this.context = SpringApplication.run(main, args);
	}

}