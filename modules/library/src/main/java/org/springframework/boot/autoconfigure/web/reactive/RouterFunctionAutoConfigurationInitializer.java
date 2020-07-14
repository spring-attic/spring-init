package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.util.ClassUtils;

public class RouterFunctionAutoConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {
	private static final boolean enabled;

	static {
		enabled = ClassUtils.isPresent("org.springframework.web.reactive.config.WebFluxConfigurer", null);
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		if (RouterFunctionAutoConfigurationInitializer.enabled) {
			ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
			if (conditions.matches(WebFluxAutoConfiguration.class)) {
				if (context.getBeanFactory().getBeanNamesForType(WebFluxAutoConfiguration.class).length == 0) {
					context.registerBean(WebFluxAutoConfiguration.class, () -> new WebFluxAutoConfiguration());
					if (conditions.matches(WebFluxAutoConfiguration.class, OrderedHiddenHttpMethodFilter.class)) {
						context.registerBean("hiddenHttpMethodFilter", OrderedHiddenHttpMethodFilter.class,
								() -> context.getBean(WebFluxAutoConfiguration.class).hiddenHttpMethodFilter());
					}
					new WebFluxAutoConfiguration_WelcomePageConfigurationInitializer().initialize(context);
					if (context.getBeanFactory().getBeanNamesForType(ResourceProperties.class).length == 0) {
						context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
					}
					if (context.getBeanFactory().getBeanNamesForType(WebFluxProperties.class).length == 0) {
						context.registerBean(WebFluxProperties.class, () -> new WebFluxProperties());
					}
					new RouterFunctionAutoConfiguration_EnableWebFluxConfigurationInitializer().initialize(context);
					new WebFluxAutoConfiguration_ResourceChainCustomizerConfigurationInitializer().initialize(context);
				}
			}
		}
	}
}
