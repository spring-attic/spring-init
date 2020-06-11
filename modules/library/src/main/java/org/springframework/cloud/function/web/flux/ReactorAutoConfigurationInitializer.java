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

package org.springframework.cloud.function.web.flux;

import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.cloud.function.web.BasicStringConverter;
import org.springframework.cloud.function.web.RequestProcessor;
import org.springframework.cloud.function.web.StringConverter;
import org.springframework.cloud.function.web.function.PublicFunctionEndpointFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * @author Dave Syer
 * @since 2.0
 *
 */
public class ReactorAutoConfigurationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		registerEndpoint(context);
	}

	private void registerEndpoint(GenericApplicationContext context) {
		context.registerBean(StringConverter.class,
				() -> new BasicStringConverter(context.getBean(FunctionInspector.class), context.getBeanFactory()));
		context.registerBean(RequestProcessor.class,
				() -> new RequestProcessor(context.getBean(FunctionInspector.class),
						context.getBean(FunctionCatalog.class), context.getBeanProvider(JsonMapper.class),
						context.getBean(StringConverter.class), context.getBeanProvider(ServerCodecConfigurer.class)));
		context.registerBean(PublicFunctionEndpointFactory.class,
				() -> new PublicFunctionEndpointFactory(context.getBean(FunctionCatalog.class),
						context.getBean(FunctionInspector.class), context.getBean(RequestProcessor.class),
						context.getEnvironment()));
		context.registerBean(RouterFunction.class,
				() -> context.getBean(PublicFunctionEndpointFactory.class).functionEndpoints());
	}

}