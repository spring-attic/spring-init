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
package org.springframework.init;

import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class TestModuleInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		if (!ClassUtils.isPresent(
				"org.springframework.boot.test.context.ImportsContextCustomizer",
				context.getClassLoader())
				|| !context.getEnvironment().getProperty("spring.functional.enabled",
						Boolean.class, true)) {
			// Only used in tests - could move to separate jar
			return;
		}
		if (!context.getBeanFactory()
				.containsBeanDefinition(ConditionService.class.getName())) {
			context.registerBean(ConditionService.class,
					() -> new ModuleInstallerConditionService(context,
							context.getEnvironment(), context));
			context.registerBean(ImportRegistrars.class,
					() -> new ModuleInstallerImportRegistrars(context));
		}
		for (String name : context.getBeanFactory().getBeanDefinitionNames()) {
			BeanDefinition definition = context.getBeanFactory().getBeanDefinition(name);
			if (definition.getBeanClassName()
					.contains("ImportsContextCustomizer$ImportsConfiguration")) {
				Class<?> testClass = (definition != null)
						? (Class<?>) definition.getAttribute("testClass")
						: null;
				if (testClass != null) {
					String enabled = context.getEnvironment().getProperty(
							EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY);
					if (enabled != null) {
						TestPropertyValues
								.of(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY
										+ "=true")
								.applyTo(context);
					}
					Set<Import> merged = AnnotatedElementUtils
							.findAllMergedAnnotations(testClass, Import.class);
					for (Import ann : merged) {
						for (Class<?> imported : ann.value()) {
							if (ImportSelector.class.isAssignableFrom(imported)) {
								ImportSelector selector = BeanUtils
										.instantiateClass(imported, ImportSelector.class);
								for (String selected : selector.selectImports(
										new StandardAnnotationMetadata(testClass))) {
									if (ClassUtils.isPresent(selected + "Initializer",
											null)) {
										@SuppressWarnings({ "unchecked" })
										ApplicationContextInitializer<GenericApplicationContext> initializer = BeanUtils
												.instantiateClass(
														ClassUtils.resolveClassName(
																selected + "Initializer",
																null),
														ApplicationContextInitializer.class);
										initializer.initialize(context);
									}
								}
							}
						}
					}
					if (enabled != null) {
						// TODO: maybe work out a better way to switch on and off the auto
						// configs
						TestPropertyValues
								.of(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY
										+ "=" + enabled)
								.applyTo(context);
					}
				}
			}
		}
	}

}
