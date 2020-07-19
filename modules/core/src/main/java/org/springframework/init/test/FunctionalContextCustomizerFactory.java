/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.init.func.FunctionalInstallerListener;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.init.func.SimpleConditionService;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * @author Dave Syer
 */
class FunctionalContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configurationAttributes) {
		return new FunctionalContextCustomizer();
	}
	
	class FunctionalContextCustomizer implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			if (!isEnabled(context.getEnvironment())) {
				return;
			}
			FunctionalInstallerListener.initialize((GenericApplicationContext)context);
			ImportRegistrars registrars = InfrastructureUtils.getBean(context.getBeanFactory(),
					ImportRegistrars.class.getName(), ImportRegistrars.class);
			for (String name : context.getBeanFactory().getBeanDefinitionNames()) {
				BeanDefinition definition = context.getBeanFactory().getBeanDefinition(name);
				if (definition.getBeanClassName().contains("ImportsContextCustomizer$ImportsConfiguration")) {
					SimpleConditionService.EXCLUDES_ENABLED = true;
					Class<?> testClass = (definition != null) ? (Class<?>) definition.getAttribute("testClass") : null;
					if (testClass != null) {
						Set<Import> merged = AnnotatedElementUtils.findAllMergedAnnotations(testClass, Import.class);
						for (Import ann : merged) {
							for (Class<?> imported : ann.value()) {
								registrars.add(testClass, imported);
							}
						}
					}
				}
			}
		}
		
		private boolean isEnabled(ConfigurableEnvironment environment) {
			return environment.getProperty("spring.functional.enabled", Boolean.class, true);
		}

	}

}
