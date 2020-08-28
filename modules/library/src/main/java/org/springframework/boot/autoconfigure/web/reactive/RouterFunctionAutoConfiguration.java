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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.Order;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.validation.Validator;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.filter.reactive.HiddenHttpMethodFilter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.server.WebExceptionHandler;
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

	public static class DelegateFunctionalConfiguration extends WebFluxAutoConfiguration.EnableWebFluxConfiguration {

		public DelegateFunctionalConfiguration(WebFluxProperties webFluxProperties,
				ObjectProvider<WebFluxRegistrations> webFluxRegistrations) {
			super(webFluxProperties, webFluxRegistrations);
		}

		protected ConfigurableWebBindingInitializer getConfigurableWebBindingInitializer(
				FormattingConversionService webFluxConversionService, Validator webFluxValidator) {
			return super.getConfigurableWebBindingInitializer(webFluxConversionService, webFluxValidator);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties({ ResourceProperties.class, WebFluxProperties.class })
	public static class EnableFunctionalConfiguration implements ApplicationContextAware {

		private DelegateFunctionalConfiguration delegate;

		@Override
		public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
			delegate.setApplicationContext(applicationContext);
		}

		public EnableFunctionalConfiguration(WebFluxProperties webFluxProperties,
				ObjectProvider<WebFluxRegistrations> webFluxRegistrations) {
			delegate = new DelegateFunctionalConfiguration(webFluxProperties, webFluxRegistrations);
		}

		@Bean
		public DispatcherHandler webHandler() {
			return delegate.webHandler();
		}

		@Bean
		@Order(0)
		public WebExceptionHandler responseStatusExceptionHandler() {
			return delegate.responseStatusExceptionHandler();
		}

		@Bean
		public RequestedContentTypeResolver webFluxContentTypeResolver() {
			return delegate.webFluxContentTypeResolver();
		}

		@Bean
		public RouterFunctionMapping routerFunctionMapping(ServerCodecConfigurer serverCodecConfigurer) {
			return delegate.routerFunctionMapping(serverCodecConfigurer);
		}

		@Bean
		public ResourceUrlProvider resourceUrlProvider() {
			return delegate.resourceUrlProvider();
		}

		@Bean
		public ServerCodecConfigurer serverCodecConfigurer() {
			return delegate.serverCodecConfigurer();
		}

		@Bean
		public LocaleContextResolver localeContextResolver() {
			return delegate.localeContextResolver();
		}

		@Bean
		public ReactiveAdapterRegistry webFluxAdapterRegistry() {
			return delegate.webFluxAdapterRegistry();
		}

		@Bean
		public HandlerFunctionAdapter handlerFunctionAdapter() {
			return delegate.handlerFunctionAdapter();
		}

		@Bean
		public SimpleHandlerAdapter simpleHandlerAdapter() {
			return delegate.simpleHandlerAdapter();
		}

		@Bean
		public ServerResponseResultHandler serverResponseResultHandler(ServerCodecConfigurer serverCodecConfigurer) {
			return delegate.serverResponseResultHandler(serverCodecConfigurer);
		}

		@Bean
		public ResponseEntityResultHandler responseEntityResultHandler(
				@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
				ServerCodecConfigurer serverCodecConfigurer,
				@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {
			return delegate.responseEntityResultHandler(reactiveAdapterRegistry, serverCodecConfigurer, contentTypeResolver);
		}

		@Bean
		public ResponseBodyResultHandler responseBodyResultHandler(
				@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
				ServerCodecConfigurer serverCodecConfigurer,
				@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {
			return delegate.responseBodyResultHandler(reactiveAdapterRegistry, serverCodecConfigurer, contentTypeResolver);
		}

		@Bean
		public ViewResolutionResultHandler viewResolutionResultHandler(
				@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
				@Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver) {
			return delegate.viewResolutionResultHandler(reactiveAdapterRegistry, contentTypeResolver);
		}

		@Bean
		public FormattingConversionService webFluxConversionService() {
			return delegate.webFluxConversionService();
		}

		@Bean
		public Validator webFluxValidator() {
			return delegate.webFluxValidator();
		}

		@Bean
		public HandlerMapping resourceHandlerMapping(ResourceUrlProvider resourceUrlProvider) {
			return delegate.resourceHandlerMapping(resourceUrlProvider);
		}

		@Bean
		public WebBindingInitializer webBindingInitializer(FormattingConversionService webFluxConversionService,
				Validator webFluxValidator) {
			return delegate.getConfigurableWebBindingInitializer(webFluxConversionService, webFluxValidator);
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
