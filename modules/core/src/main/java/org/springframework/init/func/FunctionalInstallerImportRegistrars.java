/*
 * Copyright 2018 the original author or authors.
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class FunctionalInstallerImportRegistrars implements ImportRegistrars {

	private Set<Imported> imported = new LinkedHashSet<>();

	private Set<ApplicationContextInitializer<GenericApplicationContext>> deferred = new LinkedHashSet<>();

	@Override
	public void add(Class<?> importer, Class<?> imported) {
		this.imported.add(new Imported(importer, imported));
	}

	@Override
	public Set<Imported> getImports() {
		return Collections.unmodifiableSet(imported);
	}

	@Override
	public void defer(ApplicationContextInitializer<?>... initializers) {
		for (ApplicationContextInitializer<?> initializer : initializers) {
			@SuppressWarnings("unchecked")
			ApplicationContextInitializer<GenericApplicationContext> generic = (ApplicationContextInitializer<GenericApplicationContext>) initializer;
			this.deferred.add(generic);
		}
	}

	@Override
	public void processDeferred(GenericApplicationContext context) {
		Set<ApplicationContextInitializer<GenericApplicationContext>> applied = new HashSet<>();
		int count = 1;
		while (count > 0) {
			count = this.deferred.size();
			for (ApplicationContextInitializer<GenericApplicationContext> deferred : this.deferred) {
				if (!applied.contains(deferred)) {
					applied.add(deferred);
					deferred.initialize(context);
				}
			}
			count = this.deferred.size() - count;
		}
	}

}
