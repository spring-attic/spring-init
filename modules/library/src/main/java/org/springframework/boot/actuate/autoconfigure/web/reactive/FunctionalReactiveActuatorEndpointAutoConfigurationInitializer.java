package org.springframework.boot.actuate.autoconfigure.web.reactive;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.function.server.RouterFunction;

public class FunctionalReactiveActuatorEndpointAutoConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	private static final boolean enabled;

	static {
		enabled = ClassUtils.isPresent("org.springframework.boot.actuate.endpoint.annotation.Endpoint", null);
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		if (FunctionalReactiveActuatorEndpointAutoConfigurationInitializer.enabled) {
			ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
			if (conditions.matches(FunctionalReactiveActuatorEndpointAutoConfiguration.class)) {
				if (context.getBeanFactory()
						.getBeanNamesForType(FunctionalReactiveActuatorEndpointAutoConfiguration.class).length == 0) {
					context.registerBean(FunctionalReactiveActuatorEndpointAutoConfiguration.class,
							() -> new FunctionalReactiveActuatorEndpointAutoConfiguration());
					context.registerBean("actuatorEndpoints", RouterFunction.class,
							() -> context.getBean(FunctionalReactiveActuatorEndpointAutoConfiguration.class)
									.actuatorEndpoints(context.getBean(WebEndpointProperties.class),
											context.getBean(HealthEndpoint.class),
											context.getBean(InfoEndpoint.class)));
				}
			}
		}
	}

}
