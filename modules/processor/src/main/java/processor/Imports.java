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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.element.TypeElement;

/**
 * @author Dave Syer
 *
 */
public class Imports {

	private ElementUtils utils;
	private Map<TypeElement, Set<TypeElement>> imports = new ConcurrentHashMap<>();
	private Set<TypeElement> included = new LinkedHashSet<>();
	private Set<TypeElement> registrars = new LinkedHashSet<>();
	private Set<TypeElement> selectors = new LinkedHashSet<>();

	public Imports(ElementUtils utils) {
		this.utils = utils;
	}

	public void addImport(TypeElement owner, TypeElement imported) {
		this.included.add(imported);
		if (owner.equals(imported)) {
			return;
		}
		imports.computeIfAbsent(owner, key -> new LinkedHashSet<>()).add(imported);
		classify(imported);
	}

	public void addNested(TypeElement owner, TypeElement nested) {
		Set<TypeElement> set = imports.computeIfAbsent(owner,
				key -> new LinkedHashSet<>());
		set.add(nested);
		this.included.add(nested);
	}

	private void classify(TypeElement imported) {
		if (utils.implementsInterface(imported,
				SpringClassNames.IMPORT_BEAN_DEFINITION_REGISTRAR)) {
			registrars.add(imported);
		}
		else if (utils.implementsInterface(imported, SpringClassNames.IMPORT_SELECTOR)) {
			selectors.add(imported);
		}
	}

	public Set<TypeElement> getIncluded() {
		return this.included;
	}

	public Map<TypeElement, Set<TypeElement>> getImports() {
		return this.imports;
	}

	public Set<TypeElement> getImports(TypeElement type) {
		return this.imports.containsKey(type) ? this.getImports().get(type)
				: Collections.emptySet();
	}

	public Set<TypeElement> getRegistrars() {
		return this.registrars;
	}

	public Set<TypeElement> getSelectors() {
		return this.selectors;
	}

}
