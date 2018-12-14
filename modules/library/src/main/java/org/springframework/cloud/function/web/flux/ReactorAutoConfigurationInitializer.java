/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.function.web.flux;

import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.cloud.function.web.BasicStringConverter;
import org.springframework.cloud.function.web.RequestProcessor;
import org.springframework.cloud.function.web.RequestProcessor.FunctionWrapper;
import org.springframework.cloud.function.web.StringConverter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author Dave Syer
 * @since 2.0
 *
 */
public class ReactorAutoConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		registerEndpoint(context);
	}

	private void registerEndpoint(GenericApplicationContext context) {
		context.registerBean(StringConverter.class,
				() -> new BasicStringConverter(context.getBean(FunctionInspector.class),
						context.getBeanFactory()));
		context.registerBean(RequestProcessor.class,
				() -> new RequestProcessor(context.getBean(FunctionInspector.class),
						context.getBeanProvider(JsonMapper.class),
						context.getBean(StringConverter.class),
						context.getBeanProvider(ServerCodecConfigurer.class)));
		context.registerBean(FunctionEndpointFactory.class,
				() -> new FunctionEndpointFactory(context.getBean(FunctionCatalog.class),
						context.getBean(FunctionInspector.class),
						context.getBean(RequestProcessor.class),
						context.getEnvironment()));
		context.registerBean(RouterFunction.class,
				() -> context.getBean(FunctionEndpointFactory.class).functionEndpoints());
	}

}

class FunctionEndpointFactory {

	private static Log logger = LogFactory.getLog(FunctionEndpointFactory.class);

	private Function<Flux<?>, Flux<?>> function;

	private FunctionInspector inspector;

	private RequestProcessor processor;

	public FunctionEndpointFactory(FunctionCatalog catalog, FunctionInspector inspector,
			RequestProcessor processor, Environment environment) {
		String handler = environment.resolvePlaceholders("${function.handler}");
		if (handler.startsWith("$")) {
			handler = null;
		}
		this.processor = processor;
		this.inspector = inspector;
		this.function = extract(catalog, handler);
	}

	private Function<Flux<?>, Flux<?>> extract(FunctionCatalog catalog, String handler) {
		Set<String> names = catalog.getNames(Function.class);
		if (!names.isEmpty()) {
			logger.info("Found functions: " + names);
			if (handler != null) {
				logger.info("Configured function: " + handler);
				Assert.isTrue(names.contains(handler),
						"Cannot locate function: " + handler);
				return catalog.lookup(Function.class, handler);
			}
			return catalog.lookup(Function.class, names.iterator().next());
		}
		throw new IllegalStateException("No function defined");
	}

	@SuppressWarnings({ "unchecked" })
	public <T> RouterFunction<?> functionEndpoints() {
		return route(POST("/"), request -> {
			Class<T> outputType = (Class<T>) this.inspector.getOutputType(this.function);
			FunctionWrapper wrapper = RequestProcessor.wrapper(function, null, null);
			Mono<ResponseEntity<?>> stream = request.bodyToMono(String.class)
					.flatMap(content -> processor.post(wrapper, content, false));
			return stream.flatMap(entity -> status(entity.getStatusCode())
					.headers(headers -> headers.addAll(entity.getHeaders()))
					.body(Mono.just((T) entity.getBody()), outputType));
		});
	}

}