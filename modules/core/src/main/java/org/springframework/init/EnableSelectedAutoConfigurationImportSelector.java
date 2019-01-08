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

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Dave Syer
 *
 */
public class EnableSelectedAutoConfigurationImportSelector
		extends AutoConfigurationImportSelector {

	private static final String[] NO_IMPORTS = {};

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		if (!isEnabled(metadata)) {
			return NO_IMPORTS;
		}
		// Don't use super.selectImports() because it does too much work with the
		// annotation metadata
		return (String[]) metadata.getAnnotationAttributes(
				EnableSelectedAutoConfiguration.class.getName(), true).get("value");
	}

	@Override
	protected boolean isEnabled(AnnotationMetadata metadata) {
		return super.isEnabled(metadata) && getEnvironment().getProperty(
				EnableSelectedAutoConfiguration.ENABLED_OVERRIDE_PROPERTY, Boolean.class,
				true);
	}

	@Override
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		String[] values = (String[]) metadata.getAnnotationAttributes(
				EnableSelectedAutoConfiguration.class.getName(), true).get("value");
		return Arrays.asList(values);
	}

	@Override
	protected Class<?> getAnnotationClass() {
		return EnableSelectedAutoConfiguration.class;
	}
}
