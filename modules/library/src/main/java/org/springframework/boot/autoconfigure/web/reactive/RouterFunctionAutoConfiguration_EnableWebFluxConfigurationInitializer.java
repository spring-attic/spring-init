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
				.getBeanNamesForType(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).length == 0) {
			context.registerBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class,
					() -> new WebFluxAutoConfiguration.EnableWebFluxConfiguration(
							context.getBean(WebFluxProperties.class),
							context.getBeanProvider(WebFluxRegistrations.class)));
			context.registerBean("webFluxConversionService", FormattingConversionService.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).webFluxConversionService());
			context.registerBean("webFluxValidator", Validator.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).webFluxValidator());
			context.registerBean("webHandler", DispatcherHandler.class,
					() -> context.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).webHandler());
			context.registerBean("responseStatusExceptionHandler", WebExceptionHandler.class,
					() -> context.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class)
							.responseStatusExceptionHandler());
			context.registerBean("webFluxContentTypeResolver", RequestedContentTypeResolver.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).webFluxContentTypeResolver());
			context.registerBean("routerFunctionMapping", RouterFunctionMapping.class,
					() -> context.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class)
							.routerFunctionMapping(context.getBean(ServerCodecConfigurer.class)));
			context.registerBean("resourceHandlerMapping", HandlerMapping.class,
					() -> context.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class)
							.resourceHandlerMapping(context.getBean(ResourceUrlProvider.class)));
			context.registerBean("resourceUrlProvider", ResourceUrlProvider.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).resourceUrlProvider());
			context.registerBean("serverCodecConfigurer", ServerCodecConfigurer.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).serverCodecConfigurer());
			context.registerBean("localeContextResolver", LocaleContextResolver.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).localeContextResolver());
			context.registerBean("webFluxAdapterRegistry", ReactiveAdapterRegistry.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).webFluxAdapterRegistry());
			context.registerBean("handlerFunctionAdapter", HandlerFunctionAdapter.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).handlerFunctionAdapter());
			context.registerBean("simpleHandlerAdapter", SimpleHandlerAdapter.class, () -> context
					.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class).simpleHandlerAdapter());
			context.registerBean("serverResponseResultHandler", ServerResponseResultHandler.class,
					() -> context.getBean(WebFluxAutoConfiguration.EnableWebFluxConfiguration.class)
							.serverResponseResultHandler(context.getBean(ServerCodecConfigurer.class)));
		}
	}
}
