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

package org.springframework.init.tools;

import java.io.IOException;

import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.InfrastructureUtils;

/**
 * @author Dave Syer
 *
 */
public class OnClassConditionService implements ConditionService {

	private final ConditionEvaluator evaluator;

	private MetadataReaderFactory metadataReaderFactory;

	public OnClassConditionService(GenericApplicationContext context) {
		this.evaluator = new ConditionEvaluator(context, context, context.getEnvironment(), context);
		String metadataFactory = MetadataReaderFactory.class.getName();
		this.metadataReaderFactory = InfrastructureUtils.containsBean(context.getBeanFactory(), metadataFactory)
				? (MetadataReaderFactory) InfrastructureUtils.getBean(context.getBeanFactory(), MetadataReaderFactory.class)
				: new CachingMetadataReaderFactory(context.getClassLoader());
	}

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		try {
			return !this.evaluator.shouldSkip(getMetadata(type));
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
		return true;
	}

	@Override
	public boolean includes(Class<?> type) {
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
