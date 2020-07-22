package org.springframework.cloud.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

public class PropertySourceBootstrapConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		if (context.getBeanFactory().getBeanNamesForType(PropertySourceBootstrapConfiguration.class).length == 0) {
			ConfigurationPropertiesBindingPostProcessor.register(context);
			context.registerBean(PropertySourceBootstrapProperties.class, () -> new PropertySourceBootstrapProperties());
			context.registerBean(PropertySourceBootstrapConfiguration.class,
					() -> new PropertySourceBootstrapConfiguration());
		}
	}

}
