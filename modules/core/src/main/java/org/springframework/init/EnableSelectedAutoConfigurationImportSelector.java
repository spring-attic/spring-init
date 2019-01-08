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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

/**
 * @author Dave Syer
 *
 */
public class EnableSelectedAutoConfigurationImportSelector
		extends AutoConfigurationImportSelector {

	private static Log logger = LogFactory
			.getLog(EnableSelectedAutoConfigurationImportSelector.class);

	private static final String[] NO_IMPORTS = {};

	private static Map<String, Set<String>> mappings = new HashMap<>();

	private static Set<String> mapped = new HashSet<>();

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		if (!isEnabled(metadata)) {
			return NO_IMPORTS;
		}
		// Don't use super.selectImports() because it does too much work with the
		// annotation metadata
		return computeImports(metadata).toArray(new String[0]);
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
		return computeImports(metadata);
	}

	private List<String> computeImports(AnnotationMetadata metadata) {
		String[] values = (String[]) metadata.getAnnotationAttributes(
				EnableSelectedAutoConfiguration.class.getName(), true).get("value");
		Set<String> result = new LinkedHashSet<>();
		for (String value : values) {
			StopWatch stop = new StopWatch("selected");
			if (!mapped.contains(value)) {
				mapped.add(value);
				if (!ClassUtils.isPresent(value, null)) {
					continue;
				}
				Class<?> resolved = ClassUtils.resolveClassName(value, null);
				if (AnnotationUtils.isAnnotationDeclaredLocally(
						SelectedAutoConfiguration.class, resolved)) {
					stop.start(value);
					for (SelectedAutoConfiguration selected : findSelected(resolved)) {
						AnnotationAttributes attrs = AnnotationUtils
								.getAnnotationAttributes(selected, true, false);
						String[] mapped = attrs.getStringArray("value");
						EnableSelectedAutoConfigurationImportSelector.mappings
								.computeIfAbsent(value, k -> new LinkedHashSet<>())
								.addAll(Arrays.asList(mapped));
					}
					stop.stop();
				}
				if (stop.getTaskCount() > 0) {
					logger.info("Initialized autoconfig mappings: " + stop);
				}
			}
			if (EnableSelectedAutoConfigurationImportSelector.mappings
					.containsKey(value)) {
				result.addAll(EnableSelectedAutoConfigurationImportSelector.mappings
						.get(value));
			}
			else {
				result.add(value);
			}
		}
		return new ArrayList<>(result);

	}

	private Collection<SelectedAutoConfiguration> findSelected(Class<?> type) {
		if (!AnnotationUtils.isAnnotationDeclaredLocally(SelectedAutoConfiguration.class,
				type)) {
			return Collections.emptySet();
		}
		Set<SelectedAutoConfiguration> list = new LinkedHashSet<>();
		SelectedAutoConfiguration configs = AnnotationUtils.findAnnotation(type,
				SelectedAutoConfiguration.class);
		list.add(configs);
		for (Class<?> depend : configs.depends()) {
			list.addAll(findSelected(depend));
		}
		return list;
	}

	@Override
	protected Class<?> getAnnotationClass() {
		return EnableSelectedAutoConfiguration.class;
	}
}
