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
package processor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.TypeElement;

/**
 * @author Dave Syer
 *
 */
public class Components {

	private ElementUtils utils;
	private Set<TypeElement> components = new LinkedHashSet<>();
	private Set<TypeElement> scans = new LinkedHashSet<>();
	private Map<String, Set<TypeElement>> packages = new LinkedHashMap<>();

	public Components(ElementUtils utils) {
		this.utils = utils;
	}

	public void addComponent(TypeElement type) {
		if (utils.hasAnnotation(type, SpringClassNames.COMPONENT.toString())) {
			this.components.add(type);
			String pkg = utils.getPackage(type);
			while (pkg.length() > 0) {
				packages.computeIfAbsent(pkg, key -> new LinkedHashSet<>()).add(type);
				pkg = pkg.lastIndexOf(".") > 0 ? pkg.substring(0, pkg.lastIndexOf("."))
						: "";
			}
		}
		if (utils.hasAnnotation(type, SpringClassNames.COMPONENT_SCAN.toString())) {
			addScan(type);
		}
	}

	private void addScan(TypeElement owner) {
		this.scans.add(owner);
	}

	public Set<TypeElement> getAll() {
		return this.components;
	}

	public Map<TypeElement, Set<TypeElement>> getComponents() {
		Map<TypeElement, Set<TypeElement>> result = new LinkedHashMap<>();
		for (TypeElement owner : scans) {
			Set<TypeElement> computed = result.computeIfAbsent(owner,
					key -> new LinkedHashSet<>());
			for (String pkg : getBasePackage(owner)) {
				computed.addAll(packages.get(pkg));
			}
		}
		return result;
	}

	private Set<String> getBasePackage(TypeElement owner) {
		Set<String> result = new LinkedHashSet<>();
		List<TypeElement> bases = utils.getTypesFromAnnotation(owner,
				SpringClassNames.COMPONENT_SCAN.toString(), "basePackageClasses");
		for (TypeElement base : bases) {
			String pkg = utils.getPackage(base);
			result.add(pkg);
		}
		result.addAll(utils.getStringsFromAnnotation(owner,
				SpringClassNames.COMPONENT_SCAN.toString(), "basePackages"));
		if (result.isEmpty()) {
			result.add(utils.getPackage(owner));
		}
		return result;
	}
}
