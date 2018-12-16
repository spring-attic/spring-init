/*
 * Copyright 2018 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class ModuleInstallerConditionService implements ConditionService {

	private final ConditionEvaluator evaluator;
	private final ClassLoader classLoader;
	private final Map<Class<?>, StandardAnnotationMetadata> metadata = new ConcurrentHashMap<>();

	public ModuleInstallerConditionService(BeanDefinitionRegistry registry,
			Environment environment, ResourceLoader resourceLoader) {
		this.evaluator = new ConditionEvaluator(registry, environment, resourceLoader);
		this.classLoader = resourceLoader.getClassLoader();
	}

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		try {
			return !this.evaluator.shouldSkip(getMetadata(type), phase);
		}
		catch (ArrayStoreException e) {
			return false;
		}
	}

	@Override
	public boolean matches(Class<?> type) {
		return matches(type, (ConfigurationPhase) null);
	}

	@Override
	public boolean matches(Class<?> factory, Class<?> type) {
		StandardAnnotationMetadata metadata = getMetadata(factory);
		for (MethodMetadata method : metadata.getAnnotatedMethods(Bean.class.getName())) {
			Class<?> candidate = ClassUtils.resolveClassName(method.getReturnTypeName(),
					this.classLoader);
			if (type.isAssignableFrom(candidate)) {
				return !this.evaluator.shouldSkip(method);
			}
		}
		Class<?> base = factory.getSuperclass();
		if (AnnotationUtils.isAnnotationDeclaredLocally(Configuration.class, base)) {
			return matches(base, type);
		}
		return false;
	}

	public StandardAnnotationMetadata getMetadata(Class<?> factory) {
		return metadata.computeIfAbsent(factory,
				type -> new StandardAnnotationMetadata(type));
	}

}
