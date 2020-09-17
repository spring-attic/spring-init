/*
 * Copyright 2020 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

public class SimpleInitializerLocator implements InitializerLocator {

	private Map<String, ApplicationContextInitializer<GenericApplicationContext>> map = new ConcurrentHashMap<>();

	private Map<String, Supplier<ApplicationContextInitializer<GenericApplicationContext>>> lazy = new ConcurrentHashMap<>();

	public SimpleInitializerLocator() {
		this(Collections.emptyMap());
	}

	public SimpleInitializerLocator(Map<String, ApplicationContextInitializer<GenericApplicationContext>> map) {
		this.map.putAll(map);
	}

	public SimpleInitializerLocator register(String name,
			Supplier<ApplicationContextInitializer<GenericApplicationContext>> initializer) {
		this.lazy.put(name, initializer);
		return this;
	}

	@Override
	public ApplicationContextInitializer<GenericApplicationContext> getInitializer(String name) {
		if (this.map.containsKey(name)) {
			return this.map.get(name);
		}
		if (this.lazy.containsKey(name)) {
			ApplicationContextInitializer<GenericApplicationContext> value = lazy.get(name).get();
			map.put(name, value);
			return value;
		}
		return null;
	}

}
