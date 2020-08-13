/*
 * Copyright 2019-2019 the original author or authors.
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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Implementation of a {@link ConditionContext}.
 */
public class DefaultConditionContext implements ConditionContext {

	@Nullable
	private final BeanDefinitionRegistry registry;

	@Nullable
	private final ConfigurableListableBeanFactory beanFactory;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	@Nullable
	private final ClassLoader classLoader;

	public DefaultConditionContext(@Nullable BeanDefinitionRegistry registry, @Nullable Environment environment,
			@Nullable ResourceLoader resourceLoader) {

		this.registry = registry;
		this.beanFactory = deduceBeanFactory(registry);
		this.environment = (environment != null ? environment : deduceEnvironment(registry));
		this.resourceLoader = (resourceLoader != null ? resourceLoader : deduceResourceLoader(registry));
		this.classLoader = deduceClassLoader(resourceLoader, this.beanFactory);
	}

	@Nullable
	private ConfigurableListableBeanFactory deduceBeanFactory(@Nullable BeanDefinitionRegistry source) {
		if (source instanceof ConfigurableListableBeanFactory) {
			return (ConfigurableListableBeanFactory) source;
		}
		if (source instanceof ConfigurableApplicationContext) {
			return (((ConfigurableApplicationContext) source).getBeanFactory());
		}
		return null;
	}

	private Environment deduceEnvironment(@Nullable BeanDefinitionRegistry source) {
		if (source instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) source).getEnvironment();
		}
		return new StandardEnvironment();
	}

	private ResourceLoader deduceResourceLoader(@Nullable BeanDefinitionRegistry source) {
		if (source instanceof ResourceLoader) {
			return (ResourceLoader) source;
		}
		return new DefaultResourceLoader();
	}

	@Nullable
	private ClassLoader deduceClassLoader(@Nullable ResourceLoader resourceLoader,
			@Nullable ConfigurableListableBeanFactory beanFactory) {

		if (resourceLoader != null) {
			ClassLoader classLoader = resourceLoader.getClassLoader();
			if (classLoader != null) {
				return classLoader;
			}
		}
		if (beanFactory != null) {
			return beanFactory.getBeanClassLoader();
		}
		return ClassUtils.getDefaultClassLoader();
	}

	@Override
	public BeanDefinitionRegistry getRegistry() {
		Assert.state(this.registry != null, "No BeanDefinitionRegistry available");
		return this.registry;
	}

	@Override
	@Nullable
	public ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	@Override
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

}