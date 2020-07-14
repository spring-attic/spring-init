package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.function.support.HandlerFunctionAdapter;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.util.UrlPathHelper;

public class RouterFunctionAutoConfiguration_EnableWebMvcConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {
	@Override
	public void initialize(GenericApplicationContext context) {
		if (context.getBeanFactory()
				.getBeanNamesForType(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).length == 0) {
			context.registerBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class,
					() -> new WebMvcAutoConfiguration.EnableWebMvcConfiguration(
							context.getBean(ResourceProperties.class), context.getBeanProvider(WebMvcProperties.class),
							context.getBeanProvider(WebMvcRegistrations.class), context.getBeanFactory()));
			context.registerBean("welcomePageHandlerMapping", WelcomePageHandlerMapping.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class)
							.welcomePageHandlerMapping(context, context.getBean(FormattingConversionService.class),
									context.getBean(ResourceUrlProvider.class)));
			ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
			if (conditions.matches(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class, LocaleResolver.class)) {
				context.registerBean("localeResolver", LocaleResolver.class, () -> context
						.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).localeResolver());
			}
			context.registerBean("mvcConversionService", FormattingConversionService.class, () -> context
					.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcConversionService());
			context.registerBean("mvcValidator", Validator.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcValidator());
			context.registerBean("mvcContentNegotiationManager", ContentNegotiationManager.class, () -> context
					.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcContentNegotiationManager());
			context.registerBean("mvcUrlPathHelper", UrlPathHelper.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcUrlPathHelper());
			context.registerBean("mvcPathMatcher", PathMatcher.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcPathMatcher());
			context.registerBean("routerFunctionMapping", RouterFunctionMapping.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class)
							.routerFunctionMapping(
									BeanFactoryAnnotationUtils.qualifiedBeanOfType(context,
											FormattingConversionService.class, "mvcConversionService"),
									BeanFactoryAnnotationUtils.qualifiedBeanOfType(context, ResourceUrlProvider.class,
											"mvcResourceUrlProvider")));
			context.registerBean("resourceHandlerMapping", HandlerMapping.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class)
							.resourceHandlerMapping(
									BeanFactoryAnnotationUtils.qualifiedBeanOfType(context,
											ContentNegotiationManager.class, "mvcContentNegotiationManager"),
									BeanFactoryAnnotationUtils.qualifiedBeanOfType(context,
											FormattingConversionService.class, "mvcConversionService"),
									BeanFactoryAnnotationUtils.qualifiedBeanOfType(context, ResourceUrlProvider.class,
											"mvcResourceUrlProvider")));
			context.registerBean("mvcResourceUrlProvider", ResourceUrlProvider.class, () -> context
					.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcResourceUrlProvider());
			context.registerBean("handlerFunctionAdapter", HandlerFunctionAdapter.class, () -> context
					.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).handlerFunctionAdapter());
			context.registerBean("httpRequestHandlerAdapter", HttpRequestHandlerAdapter.class, () -> context
					.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).httpRequestHandlerAdapter());
			context.registerBean("handlerExceptionResolver", HandlerExceptionResolver.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class)
							.handlerExceptionResolver(BeanFactoryAnnotationUtils.qualifiedBeanOfType(context,
									ContentNegotiationManager.class, "mvcContentNegotiationManager")));
			context.registerBean("mvcViewResolver", ViewResolver.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class)
							.mvcViewResolver(BeanFactoryAnnotationUtils.qualifiedBeanOfType(context,
									ContentNegotiationManager.class, "mvcContentNegotiationManager")));
			context.registerBean("mvcHandlerMappingIntrospector", HandlerMappingIntrospector.class, () -> context
					.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).mvcHandlerMappingIntrospector());
			context.registerBean("themeResolver", ThemeResolver.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).themeResolver());
			context.registerBean("flashMapManager", FlashMapManager.class,
					() -> context.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).flashMapManager());
			context.registerBean("viewNameTranslator", RequestToViewNameTranslator.class, () -> context
					.getBean(WebMvcAutoConfiguration.EnableWebMvcConfiguration.class).viewNameTranslator());
		}
	}
}
