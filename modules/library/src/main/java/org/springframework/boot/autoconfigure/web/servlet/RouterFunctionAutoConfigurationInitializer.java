package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.servlet.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.util.ClassUtils;

public class RouterFunctionAutoConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {
	private static final boolean enabled;

	static {
		enabled = ClassUtils.isPresent("org.springframework.web.servlet.config.annotation.WebMvcConfigurer", null);
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		if (RouterFunctionAutoConfigurationInitializer.enabled) {
			ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
			if (conditions.matches(WebMvcAutoConfiguration.class)) {
				if (context.getBeanFactory().getBeanNamesForType(RouterFunctionAutoConfiguration.class).length == 0) {
					context.registerBean(RouterFunctionAutoConfiguration.class, () -> new RouterFunctionAutoConfiguration());
					if (conditions.matches(RouterFunctionAutoConfiguration.class, OrderedHiddenHttpMethodFilter.class)) {
						context.registerBean("hiddenHttpMethodFilter", OrderedHiddenHttpMethodFilter.class,
								() -> context.getBean(RouterFunctionAutoConfiguration.class).hiddenHttpMethodFilter());
					}
					if (context.getBeanFactory().getBeanNamesForType(ResourceProperties.class).length == 0) {
						context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
					}
					if (context.getBeanFactory().getBeanNamesForType(WebMvcProperties.class).length == 0) {
						context.registerBean(WebMvcProperties.class, () -> new WebMvcProperties());
					}
					new RouterFunctionAutoConfiguration_EnableWebMvcConfigurationInitializer().initialize(context);
					new WebMvcAutoConfiguration_ResourceChainCustomizerConfigurationInitializer().initialize(context);
				}
			}
		}
	}
}
