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
package org.springframework.init.bench;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

@State(Scope.Benchmark)
public class LauncherState implements Runnable, Closeable {

	private Closeable instance;

	private URLClassLoader loader;

	private ClassLoader orig;

	private Thread runThread;

	protected Throwable error;

	private long timeout = 120000;

	private Class<?> mainClass;

	private Properties args = new Properties();

	public LauncherState(Class<?> mainClass) {
		this.mainClass = mainClass;
	}

	public void setMainClass(Class<?> mainClass) {
		this.mainClass = mainClass;
	}

	public void addProperties(String... args) {
		for (String arg : args) {
			String[] keys = arg.split("=");
			this.args.setProperty(keys[0], keys.length > 0 ? keys[1] : "");
		}
	}

	@Setup(Level.Invocation)
	public void start() throws Exception {
		System.setProperty("server.port", "0");
		System.setProperty("spring.jmx.enabled", "false");
		System.setProperty("spring.config.location",
				"file:./src/main/resources/application.properties");
		System.setProperty("spring.main.logStartupInfo", "false");
		for (Object key : this.args.keySet()) {
			System.setProperty(key.toString(), this.args.getProperty(key.toString()));
		}
	}

	public void shared() throws Exception {
		if (Closeable.class.isAssignableFrom(this.mainClass)) {
			Constructor<?> constructor = mainClass.getConstructor();
			ReflectionUtils.makeAccessible(constructor);
			instance = (Closeable) constructor.newInstance();
			((Runnable) instance).run();
		}
		else {
			instance = new LauncherApplication(mainClass);
			run();
		}
	}

	public void isolated() throws Exception {
		Class<?> mainClass = loadMainClass(this.mainClass);
		if (Closeable.class.isAssignableFrom(this.mainClass)) {
			Constructor<?> constructor = mainClass.getConstructor();
			ReflectionUtils.makeAccessible(constructor);
			instance = (Closeable) constructor.newInstance();
		}
		else {
			Class<?> appClass = mainClass.getClassLoader()
					.loadClass(LauncherApplication.class.getName());
			Constructor<?> constructor = appClass.getConstructor(Class.class);
			ReflectionUtils.makeAccessible(constructor);
			instance = (Closeable) constructor.newInstance(mainClass);
		}
		this.runThread = new Thread(() -> {
			try {
				run();
			}
			catch (Throwable ex) {
				error = ex;
			}
		});
		this.runThread.start();
		try {
			this.runThread.join(timeout);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	@TearDown(Level.Invocation)
	public void close() throws IOException {
		// CachedIntrospectionResults.clearClassLoader(getClass().getClassLoader());
		if (instance != null) {
			instance.close();
		}
		if (runThread != null) {
			runThread.setContextClassLoader(null);
			runThread = null;
		}
		if (orig != null) {
			ClassUtils.overrideThreadContextClassLoader(orig);
		}
		if (loader != null) {
			try {
				loader.close();
				loader = null;
			}
			catch (Exception e) {
				System.err.println("Failed to close loader " + e);
			}
		}
		System.gc();
		for (Object key : this.args.keySet()) {
			System.clearProperty(key.toString());
		}
	}

	@Override
	public void run() {
		((Runnable) instance).run();
	}

	private Class<?> loadMainClass(Class<?> type) throws ClassNotFoundException {
		URL[] urls = filterClassPath(
				((URLClassLoader) getClass().getClassLoader()).getURLs());
		loader = new URLClassLoader(urls, getClass().getClassLoader().getParent());
		orig = ClassUtils.overrideThreadContextClassLoader(loader);
		return loader.loadClass(type.getName());
	}

	protected URL[] filterClassPath(URL[] urls) {
		return urls;
	}

}
