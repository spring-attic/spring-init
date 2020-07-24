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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * @author Dave Syer
 *
 */
public class SimpleConditionService implements ConditionService {

	public static volatile boolean EXCLUDES_ENABLED = false;

	private static final String EXCLUDE_FILTER_BEAN_NAME = "org.springframework.boot.test.autoconfigure.filter.TypeExcludeFilters";

	private final ConditionEvaluator evaluator;

	private final TypeService types;

	private ConfigurableListableBeanFactory beanFactory;

	private MetadataReaderFactory metadataReaderFactory;

	public SimpleConditionService(GenericApplicationContext context) {
		this.beanFactory = context.getBeanFactory();
		this.evaluator = new ConditionEvaluator(context, context, context.getEnvironment(), context);
		this.types = InfrastructureUtils.getBean(beanFactory, TypeService.class);
		String metadataFactory = MetadataReaderFactory.class.getName();
		this.metadataReaderFactory = InfrastructureUtils.containsBean(beanFactory, metadataFactory)
				? (MetadataReaderFactory) InfrastructureUtils.getBean(beanFactory, MetadataReaderFactory.class)
				: new CachingMetadataReaderFactory(context.getClassLoader());
	}

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		try {
			return !this.evaluator.shouldSkip(getMetadata(type), phase);
		} catch (ArrayStoreException e) {
			return false;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public boolean matches(Class<?> type) {
		return matches(type, (ConfigurationPhase) null);
	}

	@Override
	public boolean matches(Class<?> factory, Class<?> type) {
		AnnotationMetadata metadata = getMetadata(factory);
		Set<MethodMetadata> assignable = new HashSet<>();
		for (MethodMetadata method : metadata.getAnnotatedMethods(Bean.class.getName())) {
			Class<?> candidate = types.getType(method.getReturnTypeName());
			// Look for exact match first
			if (type.equals(candidate)) {
				return !this.evaluator.shouldSkip(method);
			}
			if (type.isAssignableFrom(candidate)) {
				assignable.add(method);
			}
		}
		if (assignable.size() == 1) {
			return !this.evaluator.shouldSkip(assignable.iterator().next());
		}
		// TODO: fail if size() > 1
		Class<?> base = factory.getSuperclass();
		if (base != Object.class) {
			metadata = getMetadata(base);
			if (metadata.hasAnnotation(Configuration.class.getName())) {
				return matches(base, type);
			}
		}
		return false;
	}

	@Override
	public boolean includes(Class<?> type) {
		if (!EXCLUDES_ENABLED) {
			return true;
		}
		// TODO: split this method off into a test component?
		if (beanFactory.containsSingleton(EXCLUDE_FILTER_BEAN_NAME)) {
			TypeExcludeFilter filter = (TypeExcludeFilter) beanFactory.getSingleton(EXCLUDE_FILTER_BEAN_NAME);
			try {
				if (filter.match(metadataReaderFactory.getMetadataReader(type.getName()), metadataReaderFactory)) {
					return false;
				}
			} catch (IOException e) {
				throw new IllegalStateException("Cannot read metadata for " + type);
			}
		}
		return true;
	}

	private AnnotationMetadata getMetadata(Class<?> factory) {
		if (factory == null) {
			factory = Object.class;
		}
		try {
			return metadataReaderFactory.getMetadataReader(factory.getName()).getAnnotationMetadata();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
