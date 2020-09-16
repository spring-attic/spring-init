/*
 * Copyright 2020-2020 the original author or authors.
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

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * @author Dave Syer
 *
 */
public class InfrastructureUtils {

	private static final String CONTEXT_NAME = GenericApplicationContext.class.getName();

	static class ContextHolder {

		private GenericApplicationContext context;

		public ContextHolder(GenericApplicationContext context) {
			this.context = context;
		}

		public GenericApplicationContext getContext() {
			return this.context;
		}

	}

	public static GenericApplicationContext getContext(SingletonBeanRegistry beans) {
		Object singleton = beans.getSingleton(CONTEXT_NAME);
		if (singleton instanceof ContextHolder) {
			return ((ContextHolder) singleton).getContext();
		}
		GenericApplicationContext context = (GenericApplicationContext) singleton;
		return context;
	}

	public static <T> T getBean(SingletonBeanRegistry beans, String name, Class<T> type) {
		GenericApplicationContext context = getContext(beans);
		if (!context.isActive() && context.getBeanFactory().containsSingleton(name)) {
			@SuppressWarnings("unchecked")
			T result = (T) context.getBeanFactory().getSingleton(name);
			return result;
		}
		return context.getBean(name, type);
	}

	public static <T> T getBean(SingletonBeanRegistry beans, Class<T> type) {
		GenericApplicationContext context = getContext(beans);
		if (context != null && !context.isActive() && context.getBeanFactory().containsSingleton(type.getName())) {
			@SuppressWarnings("unchecked")
			T result = (T) context.getBeanFactory().getSingleton(type.getName());
			return result;
		}
		if (beans.containsSingleton(type.getName())) {
			@SuppressWarnings("unchecked")
			T result = (T) beans.getSingleton(type.getName());
			return result;
		}
		return context.getBean(type);
	}

	public static void install(SingletonBeanRegistry beans, GenericApplicationContext context) {
		if (!isInstalled(beans)) {
			beans.registerSingleton(CONTEXT_NAME, context);
			context.getBeanFactory().registerSingleton(CONTEXT_NAME, new ContextHolder(context));
		}
	}

	public static boolean isInstalled(SingletonBeanRegistry beans) {
		return beans.containsSingleton(CONTEXT_NAME);
	}

	public static <T> T getOrCreate(GenericApplicationContext main, String name, Class<T> type) {
		GenericApplicationContext context = getContext(main.getBeanFactory());
		if (context.getBeanNamesForType(type, false, true).length == 0) {
			if (main.isActive()) {
				T bean = main.getAutowireCapableBeanFactory().createBean(type);
				context.getBeanFactory().registerSingleton(name, bean);
			}
			else {
				// Can't use beans to do this because it probably isn't active yet
				T bean = context.getAutowireCapableBeanFactory().createBean(type);
				invokeAwareMethods(bean, context.getEnvironment(), context, context);
				context.getBeanFactory().registerSingleton(name, bean);
			}
		}
		return context.getBean(type);
	}

	public static <T> T getOrCreate(GenericApplicationContext main, Class<T> initializerType) {
		return getOrCreate(main, initializerType.getName(), initializerType);
	}

	public static Object getOrCreate(GenericApplicationContext main, String type) {
		GenericApplicationContext context = getContext(main.getBeanFactory());
		TypeService types = context.getBean(TypeService.class);
		Class<?> clazz = types.getType(type);
		return getOrCreate(main, type, clazz);
	}

	public static boolean containsBean(SingletonBeanRegistry beans, String name) {
		return getContext(beans).containsBean(name);
	}

	public static boolean containsBean(SingletonBeanRegistry beans, Class<?> type) {
		return getContext(beans).getBeanFactory().containsSingleton(type.getName())
				|| getContext(beans).getBeanNamesForType(type, false, false).length > 0;
	}

	public static <T> T invokeAwareMethods(T target, Environment environment, ResourceLoader resourceLoader,
			GenericApplicationContext registry) {

		if (target instanceof Aware) {
			if (target instanceof BeanClassLoaderAware) {
				ClassLoader classLoader = (registry instanceof ConfigurableBeanFactory
						? ((ConfigurableBeanFactory) registry).getBeanClassLoader() : resourceLoader.getClassLoader());
				if (classLoader != null) {
					((BeanClassLoaderAware) target).setBeanClassLoader(classLoader);
				}
			}
			if (target instanceof BeanFactoryAware && registry instanceof BeanFactory) {
				((BeanFactoryAware) target).setBeanFactory(registry.getBeanFactory());
			}
			if (target instanceof EnvironmentAware) {
				((EnvironmentAware) target).setEnvironment(environment);
			}
			if (target instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) target).setResourceLoader(resourceLoader);
			}
			if (target instanceof ApplicationContextAware) {
				((ApplicationContextAware) target).setApplicationContext(registry);
			}
		}

		if (target instanceof InitializingBean) {
			try {
				((InitializingBean) target).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot initialize: " + target.getClass(), e);
			}
		}

		return target;

	}

}
