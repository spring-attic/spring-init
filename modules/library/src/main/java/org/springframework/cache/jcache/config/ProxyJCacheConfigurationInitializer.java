/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.jcache.config;

import org.springframework.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor;
import org.springframework.cache.jcache.interceptor.JCacheInterceptor;
import org.springframework.cache.jcache.interceptor.JCacheOperationSource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

public class ProxyJCacheConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		context.registerBean(ProxyJCacheConfiguration.class,
				() -> new ProxyJCacheConfiguration());
		context.registerBean(BeanFactoryJCacheOperationSourceAdvisor.class,
				() -> context.getBean(ProxyJCacheConfiguration.class).cacheAdvisor());
		context.registerBean(JCacheInterceptor.class,
				() -> context.getBean(ProxyJCacheConfiguration.class).cacheInterceptor());
		context.registerBean(JCacheOperationSource.class, () -> context
				.getBean(ProxyJCacheConfiguration.class).cacheOperationSource());
	}

}
