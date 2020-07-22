package org.springframework.cloud.bootstrap;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.init.func.TypeService;

public class BootstrapImportSelectorConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		if (context.getBeanFactory().getBeanNamesForType(BootstrapImportSelectorConfiguration.class).length == 0) {
			InfrastructureUtils.getBean(context.getBeanFactory(), ImportRegistrars.class).add(
					BootstrapImportSelectorConfiguration.class,
					InfrastructureUtils.getBean(context.getBeanFactory(), TypeService.class)
							.getType("org.springframework.cloud.bootstrap.BootstrapImportSelector"));
			context.registerBean(BootstrapImportSelectorConfiguration.class,
					() -> new BootstrapImportSelectorConfiguration());
		}
	}

}
