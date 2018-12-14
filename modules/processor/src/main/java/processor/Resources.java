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
public class Resources {

	private Map<TypeElement, Set<String>> imports = new ConcurrentHashMap<>();

	public void addResource(TypeElement owner, String location) {
		imports.computeIfAbsent(owner, key -> new LinkedHashSet<>()).add(location);
	}

	public Map<TypeElement, Set<String>> getResources() {
		return this.imports;
	}

	public Set<String> getImports(TypeElement type) {
		return this.imports.containsKey(type) ? this.getResources().get(type)
				: Collections.emptySet();
	}

}
