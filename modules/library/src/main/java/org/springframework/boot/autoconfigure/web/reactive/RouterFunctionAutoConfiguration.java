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
package org.springframework.boot.autoconfigure.web.reactive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties.Format;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

/**
 * @author Dave Syer
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebFluxConfigurer.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@AutoConfigureAfter({ ReactiveWebServerFactoryAutoConfiguration.class, CodecsAutoConfiguration.class,
	ValidationAutoConfiguration.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
public class RouterFunctionAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnProperty(prefix = "spring.webflux.hiddenmethod.filter", name = "enabled", matchIfMissing = false)
	public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	@Configuration(proxyBeanMethods = false)
	public static class WelcomePageConfiguration {

		@Bean
		public RouterFunctionMapping welcomePageRouterFunctionMapping(ApplicationContext applicationContext,
				WebFluxProperties webFluxProperties, ResourceProperties resourceProperties) {
			WelcomePageRouterFunctionFactory factory = new WelcomePageRouterFunctionFactory(
					new TemplateAvailabilityProviders(applicationContext), applicationContext,
					resourceProperties.getStaticLocations(), webFluxProperties.getStaticPathPattern());
			RouterFunction<ServerResponse> routerFunction = factory.createRouterFunction();
			if (routerFunction != null) {
				RouterFunctionMapping routerFunctionMapping = new RouterFunctionMapping(routerFunction);
				routerFunctionMapping.setOrder(1);
				return routerFunctionMapping;
			}
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ ResourceProperties.class, WebFluxProperties.class })
	public static class EnableWebFluxConfiguration implements ApplicationContextAware {

		private final WebFluxProperties webFluxProperties;

		@Nullable
		private ViewResolverRegistry viewResolverRegistry;
		
		@Nullable
		private Map<String, CorsConfiguration> corsConfigurations;

		@Nullable
		private ApplicationContext applicationContext;

		@Override
		public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
			if (applicationContext != null) {
					Assert.state(!applicationContext.containsBean("mvcContentNegotiationManager"),
							"The Java/XML config for Spring MVC and Spring WebFlux cannot both be enabled, " +
							"e.g. via @EnableWebMvc and @EnableWebFlux, in the same application.");
			}
		}

		public EnableWebFluxConfiguration(WebFluxProperties webFluxProperties) {
			this.webFluxProperties = webFluxProperties;
		}

		@Bean
		public DispatcherHandler webHandler() {
			return new DispatcherHandler();
		}

		@Bean
		@Order(0)
		public WebExceptionHandler responseStatusExceptionHandler() {
			return new WebFluxResponseStatusExceptionHandler();
		}

		@Bean
		public RequestedContentTypeResolver webFluxContentTypeResolver() {
			RequestedContentTypeResolverBuilder builder = new RequestedContentTypeResolverBuilder();
			configureContentTypeResolver(builder);
			return builder.build();
		}

		protected void configureContentTypeResolver(RequestedContentTypeResolverBuilder builder) {
		}

		@Bean
		public RouterFunctionMapping routerFunctionMapping(ServerCodecConfigurer serverCodecConfigurer) {
			RouterFunctionMapping mapping = createRouterFunctionMapping();
			mapping.setOrder(-1);  // go before RequestMappingHandlerMapping
			mapping.setMessageReaders(serverCodecConfigurer.getReaders());
			mapping.setCorsConfigurations(getCorsConfigurations());

			return mapping;
		}

		protected RouterFunctionMapping createRouterFunctionMapping() {
			return new RouterFunctionMapping();
		}

		protected final Map<String, CorsConfiguration> getCorsConfigurations() {
			if (this.corsConfigurations == null) {
				this.corsConfigurations = new HashMap<>();
			}
			return this.corsConfigurations;
		}

		@Bean
		public ResourceUrlProvider resourceUrlProvider() {
			return new ResourceUrlProvider();
		}

		@Bean
		public ServerCodecConfigurer serverCodecConfigurer() {
			ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();
			configureHttpMessageCodecs(serverCodecConfigurer);
			return serverCodecConfigurer;
		}

		protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		}

		@Bean
		public LocaleContextResolver localeContextResolver() {
			return createLocaleContextResolver();
		}

		protected LocaleContextResolver createLocaleContextResolver() {
			return new AcceptHeaderLocaleContextResolver();
		}

		@Bean
		public ReactiveAdapterRegistry webFluxAdapterRegistry() {
			return new ReactiveAdapterRegistry();
		}

		@Bean
		public HandlerFunctionAdapter handlerFunctionAdapter() {
			return new HandlerFunctionAdapter();
		}

		@Bean
		public SimpleHandlerAdapter simpleHandlerAdapter() {
			return new SimpleHandlerAdapter();
		}

		@Bean
		public ServerResponseResultHandler serverResponseResultHandler(ServerCodecConfigurer serverCodecConfigurer) {
			List<ViewResolver> resolvers = new ArrayList<>();
			ServerResponseResultHandler handler = new ServerResponseResultHandler();
			handler.setMessageWriters(serverCodecConfigurer.getWriters());
			handler.setViewResolvers(resolvers);
			return handler;
		}

		@Bean
		public FormattingConversionService webFluxConversionService() {
			Format format = this.webFluxProperties.getFormat();
			WebConversionService conversionService = new WebConversionService(new DateTimeFormatters()
					.dateFormat(format.getDate()).timeFormat(format.getTime()).dateTimeFormat(format.getDateTime()));
			addFormatters(conversionService);
			return conversionService;
		}

		protected void addFormatters(FormatterRegistry registry) {
		}

		@Bean
		public Validator webFluxValidator() {
			Validator validator = getValidator();
			if (validator == null) {
				if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
					Class<?> clazz;
					try {
						String name = "org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean";
						clazz = ClassUtils.forName(name, getClass().getClassLoader());
					}
					catch (ClassNotFoundException | LinkageError ex) {
						throw new BeanInitializationException("Failed to resolve default validator class", ex);
					}
					validator = (Validator) BeanUtils.instantiateClass(clazz);
				}
				else {
					validator = new NoOpValidator();
				}
			}
			return validator;
		}

		/**
		 * Override this method to provide a custom {@link Validator}.
		 */
		@Nullable
		protected Validator getValidator() {
			return null;
		}

	}
	
	private static final class NoOpValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
		}
	}

	
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnEnabledResourceChain
	static class ResourceChainCustomizerConfiguration {

		@Bean
		ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer() {
			return new ResourceChainResourceHandlerRegistrationCustomizer();
		}

	}

}
