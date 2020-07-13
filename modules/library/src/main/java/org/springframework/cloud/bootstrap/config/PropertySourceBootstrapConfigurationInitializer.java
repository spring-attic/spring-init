package org.springframework.cloud.bootstrap.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;

public class PropertySourceBootstrapConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		if (context.getBeanFactory().getBeanNamesForType(PropertySourceBootstrapConfiguration.class).length == 0) {
			InfrastructureUtils.getBean(context.getBeanFactory(), ImportRegistrars.class).add(
					PropertySourceBootstrapConfiguration.class,
					"org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelector");
			context.registerBean(PropertySourceBootstrapConfiguration.class,
					() -> new PropertySourceBootstrapConfiguration());
		}
	}

}
