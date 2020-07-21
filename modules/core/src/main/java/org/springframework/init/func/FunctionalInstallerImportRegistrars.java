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
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class FunctionalInstallerImportRegistrars implements ImportRegistrars {

	private GenericApplicationContext context;

	private TypeService types;

	private Set<Imported> imported = new LinkedHashSet<>();

	public FunctionalInstallerImportRegistrars(GenericApplicationContext context) {
		this.context = context;
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		this.types = InfrastructureUtils.getBean(beanFactory, TypeService.class);
	}

	@Override
	public void add(Class<?> importer, Class<?> imported) {
		this.imported.add(new Imported(importer, imported));
	}

	@Override
	public Set<Imported> getImports() {
		return Collections.unmodifiableSet(imported);
	}

	@Override
	public void add(Class<?> importer, String typeName) {
		if (typeName.endsWith(".xml")) {
			this.imported.add(new Imported(importer, typeName));
		} else {
			if (isAutoConfiguration(importer, typeName) && !context.getEnvironment()
					.getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY, Boolean.class, true)) {
				return;
			}
			this.imported.add(new Imported(importer, types.getType(typeName)));
		}
	}

	private boolean isAutoConfiguration(Class<?> importer, String typeName) {
		// TODO: maybe work out a better way to detect auto configs
		return typeName.endsWith("AutoConfigurationImportSelector");
	}

}
