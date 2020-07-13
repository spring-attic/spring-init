package org.springframework.cloud.bootstrap.encrypt;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class EncryptionBootstrapConfiguration_RsaEncryptionConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
		if (conditions.matches(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class)) {
			if (context.getBeanFactory().getBeanNamesForType(
					EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class).length == 0) {
				InfrastructureUtils.getBean(context.getBeanFactory(), ImportRegistrars.class).add(
						EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class,
						"org.springframework.boot.context.properties.EnableConfigurationPropertiesRegistrar");
				context.registerBean(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class,
						() -> new EncryptionBootstrapConfiguration.RsaEncryptionConfiguration());
				if (conditions.matches(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class,
						TextEncryptor.class)) {
					context.registerBean("textEncryptor", TextEncryptor.class,
							() -> context.getBean(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class)
									.textEncryptor());
				}
			}
		}
	}

}
