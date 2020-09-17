package org.springframework.boot.actuate.autoconfigure.web.servlet;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.function.RouterFunction;

public class FunctionalServletActuatorEndpointAutoConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	private static final boolean enabled;

	static {
		enabled = ClassUtils.isPresent("org.springframework.boot.actuate.endpoint.annotation.Endpoint", null);
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		if (FunctionalServletActuatorEndpointAutoConfigurationInitializer.enabled) {
			ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
			if (conditions.matches(FunctionalServletActuatorEndpointAutoConfiguration.class)) {
				ImportRegistrars registrars = InfrastructureUtils.getBean(context.getBeanFactory(),
						ImportRegistrars.class);
				registrars.defer(main -> deferred((GenericApplicationContext) main, conditions));
			}
		}
	}

	private void deferred(GenericApplicationContext context, ConditionService conditions) {
		if (context.getBeanFactory()
				.getBeanNamesForType(FunctionalServletActuatorEndpointAutoConfiguration.class).length == 0) {
			context.registerBean(FunctionalServletActuatorEndpointAutoConfiguration.class,
					() -> new FunctionalServletActuatorEndpointAutoConfiguration());
			if (conditions.matches(FunctionalServletActuatorEndpointAutoConfiguration.class, RouterFunction.class)) {
				context.registerBean("actuatorEndpoints", RouterFunction.class,
						() -> context.getBean(FunctionalServletActuatorEndpointAutoConfiguration.class)
								.actuatorEndpoints(context.getBean(WebEndpointProperties.class),
										context.getBean(HealthEndpoint.class), context.getBean(InfoEndpoint.class)));
			}
		}
	}

}
