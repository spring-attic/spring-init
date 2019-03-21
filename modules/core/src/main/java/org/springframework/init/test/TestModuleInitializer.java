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
package org.springframework.init.test;

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.FunctionalInstallerImportRegistrars;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.SimpleConditionService;
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
		ImportRegistrars registrars;
		if (!context.getBeanFactory()
				.containsBeanDefinition(ConditionService.class.getName())) {
			registrars = new FunctionalInstallerImportRegistrars(context);
			context.registerBean(ConditionService.class,
					() -> new SimpleConditionService(context,
							context.getBeanFactory(), context.getEnvironment(), context));
			context.registerBean(ImportRegistrars.class, () -> registrars);
		}
		else {
			registrars = context.getBean(ImportRegistrars.class.getName(),
					ImportRegistrars.class);
		}
		for (String name : context.getBeanFactory().getBeanDefinitionNames()) {
			BeanDefinition definition = context.getBeanFactory().getBeanDefinition(name);
			if (definition.getBeanClassName()
					.contains("ImportsContextCustomizer$ImportsConfiguration")) {
				SimpleConditionService.EXCLUDES_ENABLED = true;
				Class<?> testClass = (definition != null)
						? (Class<?>) definition.getAttribute("testClass")
						: null;
				if (testClass != null) {
					Set<Import> merged = AnnotatedElementUtils
							.findAllMergedAnnotations(testClass, Import.class);
					for (Import ann : merged) {
						for (Class<?> imported : ann.value()) {
							registrars.add(testClass, imported);
						}
					}
				}
			}
		}
	}

}
