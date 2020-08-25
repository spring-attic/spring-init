package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.i18n.LocaleContextResolver;

public class RouterFunctionAutoConfiguration_EnableWebFluxConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {
	@Override
	public void initialize(GenericApplicationContext context) {
		if (context.getBeanFactory()
				.getBeanNamesForType(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).length == 0) {
			context.registerBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class,
					() -> new RouterFunctionAutoConfiguration.EnableFunctionalConfiguration(
							context.getBean(WebFluxProperties.class),
							context.getBeanProvider(WebFluxRegistrations.class)));
			context.registerBean("webFluxConversionService", FormattingConversionService.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).webFluxConversionService());
			context.registerBean("webFluxValidator", Validator.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).webFluxValidator());
			context.registerBean("webHandler", DispatcherHandler.class,
					() -> context.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).webHandler());
			context.registerBean("responseStatusExceptionHandler", WebExceptionHandler.class,
					() -> context.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class)
							.responseStatusExceptionHandler());
			context.registerBean("webFluxContentTypeResolver", RequestedContentTypeResolver.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).webFluxContentTypeResolver());
			context.registerBean("routerFunctionMapping", RouterFunctionMapping.class,
					() -> context.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class)
							.routerFunctionMapping(context.getBean(ServerCodecConfigurer.class)));
			context.registerBean("resourceHandlerMapping", HandlerMapping.class,
					() -> context.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class)
							.resourceHandlerMapping(context.getBean(ResourceUrlProvider.class)));
			context.registerBean("resourceUrlProvider", ResourceUrlProvider.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).resourceUrlProvider());
			context.registerBean("serverCodecConfigurer", ServerCodecConfigurer.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).serverCodecConfigurer());
			context.registerBean("localeContextResolver", LocaleContextResolver.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).localeContextResolver());
			context.registerBean("webFluxAdapterRegistry", ReactiveAdapterRegistry.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).webFluxAdapterRegistry());
			context.registerBean("handlerFunctionAdapter", HandlerFunctionAdapter.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).handlerFunctionAdapter());
			context.registerBean("simpleHandlerAdapter", SimpleHandlerAdapter.class, () -> context
					.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class).simpleHandlerAdapter());
			context.registerBean("serverResponseResultHandler", ServerResponseResultHandler.class,
					() -> context.getBean(RouterFunctionAutoConfiguration.EnableFunctionalConfiguration.class)
							.serverResponseResultHandler(context.getBean(ServerCodecConfigurer.class)));
		}
	}
}
