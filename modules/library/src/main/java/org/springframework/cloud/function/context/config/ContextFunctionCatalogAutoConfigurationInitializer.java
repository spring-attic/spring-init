package org.springframework.cloud.function.context.config;
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogInitializer.ContextFunctionCatalogBeanRegistrar;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class ContextFunctionCatalogAutoConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		try {
			ContextFunctionCatalogBeanRegistrar registrar = new ContextFunctionCatalogInitializer.ContextFunctionCatalogBeanRegistrar(
					context);
			registrar.postProcessBeanDefinitionRegistry(context);
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

}
