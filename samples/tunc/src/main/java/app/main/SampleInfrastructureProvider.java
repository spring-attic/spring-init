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
package app.main;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.embedded.JettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.UndertowWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.servlet.RouterFunctionAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfigurationInitializer;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.ConfigurationSource;
import org.springframework.init.func.InfrastructureProvider;
import org.springframework.init.func.SimpleConfigurationSource;
import org.springframework.init.func.TypeService;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

/**
 * @author Dave Syer
 *
 */
public class SampleInfrastructureProvider implements InfrastructureProvider {

	@Override
	public Collection<? extends ApplicationContextInitializer<GenericApplicationContext>> getInitializers() {
		return Arrays
				.asList(context -> context.registerBean(ConditionService.class, () -> new SampleConditionService()),
						context -> context.registerBean(ConfigurationSource.class, () -> new SimpleConfigurationSource( //
								(ApplicationContextInitializer<?>) context.getAutowireCapableBeanFactory()
										.createBean(context.getBean(TypeService.class)
												.getType("app.main.SampleApplicationInitializer")),
								new PropertyPlaceholderAutoConfigurationInitializer(), //
								new ConfigurationPropertiesAutoConfigurationInitializer(), //
								new ServletWebServerFactoryAutoConfigurationInitializer(), //
								new RouterFunctionAutoConfigurationInitializer(), //
								new DispatcherServletAutoConfigurationInitializer(), //
								new ErrorMvcAutoConfigurationInitializer() //
						)));
	}

}

class SampleConditionService implements ConditionService {

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		if (type.getName().equals(
				"org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration$ResourceChainCustomizerConfiguration")) {
			return false;
		}
		return true;
	}

	@Override
	public boolean matches(Class<?> type) {
		return matches(type, ConfigurationPhase.REGISTER_BEAN);
	}

	@Override
	public boolean matches(Class<?> factory, Class<?> type) {
		System.err.println("Matches: " + factory.getName() + ", " + type.getName());
		if (type == UndertowWebServerFactoryCustomizer.class || type == JettyWebServerFactoryCustomizer.class) {
			return false;
		}
		if (type == ForwardedHeaderTransformer.class) {
			return false;
		}
		if (type == MultipartResolver.class) {
			return false;
		}
		if (type == OrderedHiddenHttpMethodFilter.class) {
			return false;
		}
		return true;
	}

	@Override
	public boolean includes(Class<?> type) {
		return false;
	}

}
