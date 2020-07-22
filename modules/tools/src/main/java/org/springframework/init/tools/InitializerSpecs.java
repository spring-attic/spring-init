/*
 * Copyright 2016-2017 the original author or authors.
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

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Configuration;

/**
 * @author Andy Clement
 * @author Dave Syer
 */
public class InitializerSpecs {

	private Map<Class<?>, InitializerSpec> initializers = new LinkedHashMap<>();

	private ElementUtils utils;

	private Imports imports;

	private Components components;

	public InitializerSpecs(ElementUtils utils, Imports imports, Components components) {
		this.utils = utils;
		this.imports = imports;
		this.components = components;
	}

	public Set<InitializerSpec> getInitializers() {
		return new LinkedHashSet<>(this.initializers.values());
	}

	public void addInitializer(Class<?> initializer) {
		if (initializers.containsKey(initializer)) {
			return;
		}
		initializers.put(initializer, new InitializerSpec(this, this.utils, initializer, imports, components));
		findNestedInitializers(initializer, new HashSet<>());
	}

	private void findNestedInitializers(Class<?> type, Set<Class<?>> types) {
		if (!types.contains(type) && !Modifier.isAbstract(type.getModifiers())
				&& utils.hasAnnotation(type, Configuration.class.getName())) {
			try {
				for (Class<?> element : type.getDeclaredClasses()) {
					if (Modifier.isStatic(element.getModifiers())) {
						if (utils.hasAnnotation(element, SpringClassNames.CONFIGURATION.toString())) {
							imports.addNested(type, element);
						}
						findNestedInitializers(element, types);
					}
				}
			} catch (NoClassDefFoundError e) {
				return;
			}
			addInitializer(type);
			types.add(type);
		}

	}

}
