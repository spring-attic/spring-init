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

import java.util.List;

import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.init.select.EnableSelectedAutoConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * @author Dave Syer
 *
 */
public class TestAutoConfigurationContextCustomizerFactory
		implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configurationAttributes) {
		OverrideAutoConfiguration annotation = AnnotatedElementUtils
				.findMergedAnnotation(testClass, OverrideAutoConfiguration.class);
		if (annotation != null && !annotation.enabled()) {
			return new DisableAutoConfigurationContextCustomizer();
		}
		return null;
	}

	/**
	 * {@link ContextCustomizer} to disable full auto-configuration.
	 */
	private static class DisableAutoConfigurationContextCustomizer
			implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context,
				MergedContextConfiguration mergedConfig) {
			TestPropertyValues.of(
					EnableSelectedAutoConfiguration.ENABLED_OVERRIDE_PROPERTY + "=false")
					.applyTo(context);
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null && obj.getClass() == getClass());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
