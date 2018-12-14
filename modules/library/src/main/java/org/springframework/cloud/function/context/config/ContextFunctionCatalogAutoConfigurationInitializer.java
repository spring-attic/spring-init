package org.springframework.cloud.function.context.config;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.InMemoryFunctionCatalog;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class ContextFunctionCatalogAutoConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		try {
			register(context, context.getDefaultListableBeanFactory());
		}
		catch (BeansException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BeanCreationException("Cannot register from " + getClass(), e);
		}
	}

	protected void register(GenericApplicationContext context,
			ConfigurableListableBeanFactory factory) throws Exception {

		if (ClassUtils.isPresent("com.google.gson.Gson", null)
				&& "gson".equals(context.getEnvironment().getProperty(
						ContextFunctionCatalogAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
						"gson"))) {
			if (context.getBeanFactory().getBeanNamesForType(Gson.class, false,
					false).length == 0) {
				context.registerBean(Gson.class, () -> new Gson());
			}
			context.registerBean(JsonMapper.class,
					() -> new ContextFunctionCatalogAutoConfiguration.GsonConfiguration()
							.jsonMapper(context.getBean(Gson.class)));
		}
		else if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
				null)) {
			if (context.getBeanFactory().getBeanNamesForType(ObjectMapper.class, false,
					false).length == 0) {
				context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
			}
			context.registerBean(JsonMapper.class,
					() -> new ContextFunctionCatalogAutoConfiguration.JacksonConfiguration()
							.jsonMapper(context.getBean(ObjectMapper.class)));

		}

		if (context.getBeanFactory().getBeanNamesForType(FunctionCatalog.class, false,
				false).length == 0) {
			context.registerBean(InMemoryFunctionCatalog.class,
					() -> new InMemoryFunctionCatalog());
			context.registerBean(FunctionRegistrationPostProcessor.class,
					() -> new FunctionRegistrationPostProcessor(
							context.getAutowireCapableBeanFactory()
									.getBeanProvider(FunctionRegistration.class)));
		}
	}

	private class FunctionRegistrationPostProcessor implements BeanPostProcessor {
		@SuppressWarnings("rawtypes")
		private final ObjectProvider<FunctionRegistration> functions;

		public FunctionRegistrationPostProcessor(
				@SuppressWarnings("rawtypes") ObjectProvider<FunctionRegistration> functions) {
			this.functions = functions;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof FunctionRegistry) {
				FunctionRegistry catalog = (FunctionRegistry) bean;
				for (FunctionRegistration<?> registration : functions) {
					Assert.notEmpty(registration.getNames(),
							"FunctionRegistration must define at least one name. Was empty");
					if (registration.getType() == null) {
						throw new IllegalStateException(
								"You need an explicit type for the function: "
										+ registration.getNames());
						// TODO: in principle Spring could know how to extract this
						// from the supplier, but in practice there is no functional
						// bean registration with parametric types.
					}
					catalog.register(registration);
				}
			}
			return bean;
		}
	}

}
