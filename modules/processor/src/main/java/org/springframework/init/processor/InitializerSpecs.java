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
package org.springframework.init.processor;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * @author Andy Clement
 * @author Dave Syer
 */
public class InitializerSpecs {

	private Map<TypeElement, InitializerSpec> initializers = new LinkedHashMap<>();

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

	public void addInitializer(TypeElement initializer) {
		if (initializers.containsKey(initializer)) {
			return;
		}
		initializers.put(initializer,
				new InitializerSpec(this.utils, initializer, imports, components));
		findNestedInitializers(initializer, new HashSet<>());
	}

	private void findNestedInitializers(TypeElement type, Set<TypeElement> types) {
		if (!types.contains(type) && type.getKind() == ElementKind.CLASS
				&& !type.getModifiers().contains(Modifier.ABSTRACT)
				&& utils.hasAnnotation(type, SpringClassNames.CONFIGURATION.toString())) {
			addInitializer(type);
			for (Element element : type.getEnclosedElements()) {
				if (element instanceof TypeElement
						&& element.getModifiers().contains(Modifier.STATIC)) {
					TypeElement nested = (TypeElement) element;
					if (utils.hasAnnotation(nested,
							SpringClassNames.CONFIGURATION.toString())) {
						imports.addNested(type, nested);
					}
					findNestedInitializers(nested, types);
				}
			}
			types.add(type);
		}

	}

}
