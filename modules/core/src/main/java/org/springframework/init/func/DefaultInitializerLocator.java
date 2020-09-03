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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;

public class DefaultInitializerLocator implements InitializerLocator {

	private TypeService types;

	private GenericApplicationContext context;

	private Map<String, ApplicationContextInitializer<GenericApplicationContext>> map = new HashMap<>();

	private Collection<InitializerLocator> loaders = new HashSet<>();

	public DefaultInitializerLocator(GenericApplicationContext context) {
		this.types = InfrastructureUtils.getBean(context.getBeanFactory(), TypeService.class);
		this.context = context;
		ServiceLoader.load(InitializerLocator.class, ClassUtils.getDefaultClassLoader())
				.forEach(initializer -> this.loaders.add(initializer));
	}
	
	public DefaultInitializerLocator register(InitializerLocator locator) {
		this.loaders.add(locator);
		return this;
	}

	@Override
	public ApplicationContextInitializer<GenericApplicationContext> getInitializer(String name) {
		for (InitializerLocator locator : this.loaders) {
			ApplicationContextInitializer<GenericApplicationContext> initializer = locator.getInitializer(name);
			if (initializer != null) {
				return initializer;
			}
		}
		if (this.map.containsKey(name)) {
			return this.map.get(name);
		}
		// recurse?
		String target = name.replace("$", "_") + "Initializer";
		if (types.isPresent(target)) {
			Class<?> initializerType = types.getType(target);
			@SuppressWarnings("unchecked")
			ApplicationContextInitializer<GenericApplicationContext> initializer = (ApplicationContextInitializer<GenericApplicationContext>) InfrastructureUtils
					.getOrCreate(context, initializerType);
			map.put(name, initializer);
			return initializer;
		}
		return null;
	}

}
