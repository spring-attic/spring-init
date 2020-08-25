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
package org.springframework.boot.autoconfigure.web.servlet;

import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ConditionalOnEnabledResourceChain;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.ResourceChainResourceHandlerRegistrationCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.boot.web.servlet.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.util.UrlPathHelper;

/**
 * @author Dave Syer
 *
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter({ DispatcherServletAutoConfiguration.class, TaskExecutionAutoConfiguration.class,
		ValidationAutoConfiguration.class })
@EnableConfigurationProperties({ ResourceProperties.class, WebMvcProperties.class })
public class RouterFunctionAutoConfiguration {

	private static final String[] SERVLET_LOCATIONS = { "/" };

	@Bean
	@ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
	@ConditionalOnProperty(prefix = "spring.mvc.hiddenmethod.filter", name = "enabled", matchIfMissing = false)
	public OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
		return new OrderedHiddenHttpMethodFilter();
	}

	@Bean
	@ConditionalOnMissingBean(FormContentFilter.class)
	@ConditionalOnProperty(prefix = "spring.mvc.formcontent.filter", name = "enabled", matchIfMissing = true)
	public OrderedFormContentFilter formContentFilter() {
		return new OrderedFormContentFilter();
	}

	static String[] getResourceLocations(String[] staticLocations) {
		String[] locations = new String[staticLocations.length + SERVLET_LOCATIONS.length];
		System.arraycopy(staticLocations, 0, locations, 0, staticLocations.length);
		System.arraycopy(SERVLET_LOCATIONS, 0, locations, staticLocations.length, SERVLET_LOCATIONS.length);
		return locations;
	}

	@Configuration(proxyBeanMethods = false)
	public static class EnableFunctionalConfiguration implements ResourceLoaderAware, ApplicationContextAware, ServletContextAware {

		private WebMvcAutoConfiguration.EnableWebMvcConfiguration delegate;
		@Nullable
		private List<Object> interceptors;

		public EnableFunctionalConfiguration(ResourceProperties resourceProperties,
				ObjectProvider<WebMvcProperties> mvcPropertiesProvider,
				ObjectProvider<WebMvcRegistrations> mvcRegistrationsProvider, ListableBeanFactory beanFactory) {
			delegate = new WebMvcAutoConfiguration.EnableWebMvcConfiguration(resourceProperties, mvcPropertiesProvider,
					mvcRegistrationsProvider, beanFactory);
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			delegate.setResourceLoader(resourceLoader);
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			delegate.setApplicationContext(applicationContext);
		}
		
		@Override
		public void setServletContext(ServletContext servletContext) {
			delegate.setServletContext(servletContext);
		}

		@Bean
		public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
				FormattingConversionService mvcConversionService, ResourceUrlProvider mvcResourceUrlProvider) {
			return delegate.welcomePageHandlerMapping(applicationContext, mvcConversionService, mvcResourceUrlProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "spring.mvc", name = "locale", matchIfMissing = true)
		public LocaleResolver localeResolver() {
			return delegate.localeResolver();
		}

		@Bean
		public FormattingConversionService mvcConversionService() {
			return delegate.mvcConversionService();
		}

		@Bean
		public Validator mvcValidator() {
			return delegate.mvcValidator();
		}

		@Bean
		public ContentNegotiationManager mvcContentNegotiationManager() {
			return delegate.mvcContentNegotiationManager();
		}

		@Bean
		public UrlPathHelper mvcUrlPathHelper() {
			return delegate.mvcUrlPathHelper();
		}

		@Bean
		public PathMatcher mvcPathMatcher() {
			return delegate.mvcPathMatcher();
		}

		@Bean
		public RouterFunctionMapping routerFunctionMapping(
				@Qualifier("mvcConversionService") FormattingConversionService conversionService,
				@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
			return delegate.routerFunctionMapping(conversionService, resourceUrlProvider);
		}

		@Bean
		@Nullable
		public HandlerMapping resourceHandlerMapping(
				@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,
				@Qualifier("mvcConversionService") FormattingConversionService conversionService,
				@Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
			return delegate.resourceHandlerMapping(contentNegotiationManager, conversionService, resourceUrlProvider);
		}

		@Bean
		public ResourceUrlProvider mvcResourceUrlProvider() {
			return delegate.mvcResourceUrlProvider();
		}

		@Bean
		public HandlerFunctionAdapter handlerFunctionAdapter() {
			return delegate.handlerFunctionAdapter();
		}

		@Bean
		public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
			return delegate.httpRequestHandlerAdapter();
		}

		@Bean
		public HandlerExceptionResolver handlerExceptionResolver(
				@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
			return delegate.handlerExceptionResolver(contentNegotiationManager);
		}

		@Bean
		public ViewResolver mvcViewResolver(
				@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {
			return delegate.mvcViewResolver(contentNegotiationManager);
		}

		@Bean
		@Lazy
		public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
			return delegate.mvcHandlerMappingIntrospector();
		}

		@Bean
		public ThemeResolver themeResolver() {
			return delegate.themeResolver();
		}

		@Bean
		public FlashMapManager flashMapManager() {
			return delegate.flashMapManager();
		}

		@Bean
		public RequestToViewNameTranslator viewNameTranslator() {
			return delegate.viewNameTranslator();
		}

		@Bean
		public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
			return delegate.simpleControllerHandlerAdapter();
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
