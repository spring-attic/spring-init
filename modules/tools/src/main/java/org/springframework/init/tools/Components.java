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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dave Syer
 *
 */
public class Components {

	private ElementUtils utils;
	private Set<Class<?>> components = new LinkedHashSet<>();
	private Set<Class<?>> scans = new LinkedHashSet<>();
	private Map<String, Set<Class<?>>> packages = new LinkedHashMap<>();

	public Components(ElementUtils utils) {
		this.utils = utils;
	}

	public void addComponent(Class<?> type) {
		if (utils.hasAnnotation(type, SpringClassNames.COMPONENT.toString())) {
			this.components.add(type);
			String pkg = utils.getPackage(type);
			while (pkg.length() > 0) {
				packages.computeIfAbsent(pkg, key -> new LinkedHashSet<>()).add(type);
				pkg = pkg.lastIndexOf(".") > 0 ? pkg.substring(0, pkg.lastIndexOf(".")) : "";
			}
		}
		if (utils.hasAnnotation(type, SpringClassNames.COMPONENT_SCAN.toString())) {
			addScan(type);
		}
	}

	private void addScan(Class<?> owner) {
		this.scans.add(owner);
	}

	public Set<Class<?>> getAll() {
		return this.components;
	}

	public Map<Class<?>, Set<Class<?>>> getComponents() {
		Map<Class<?>, Set<Class<?>>> result = new LinkedHashMap<>();
		for (Class<?> owner : scans) {
			Set<Class<?>> computed = result.computeIfAbsent(owner, key -> new LinkedHashSet<>());
			for (String pkg : getBasePackage(owner)) {
				Set<Class<?>> added = packages.get(pkg);
				if (added != null) {
					computed.addAll(added);
				}
			}
		}
		return result;
	}

	private Set<String> getBasePackage(Class<?> owner) {
		Set<String> result = new LinkedHashSet<>();
		List<Class<?>> bases = utils.getTypesFromAnnotation(owner, SpringClassNames.COMPONENT_SCAN.toString(),
				"basePackageClasses");
		for (Class<?> base : bases) {
			String pkg = utils.getPackage(base);
			result.add(pkg);
		}
		result.addAll(
				utils.getStringsFromAnnotation(owner, SpringClassNames.COMPONENT_SCAN.toString(), "basePackages"));
		if (result.isEmpty()) {
			result.add(utils.getPackage(owner));
		}
		return result;
	}
}
