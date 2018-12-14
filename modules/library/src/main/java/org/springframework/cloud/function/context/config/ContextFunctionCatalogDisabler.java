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
package org.springframework.cloud.function.context.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class ContextFunctionCatalogDisabler
		implements ApplicationContextInitializer<GenericApplicationContext>, Ordered {

	@Override
	public void initialize(GenericApplicationContext context) {
		if (ClassUtils.isPresent(
				"org.springframework.cloud.function.context.config.ContextFunctionCatalogInitializer",
				context.getClassLoader())) {
			ContextFunctionCatalogInitializer.enabled = false;
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
