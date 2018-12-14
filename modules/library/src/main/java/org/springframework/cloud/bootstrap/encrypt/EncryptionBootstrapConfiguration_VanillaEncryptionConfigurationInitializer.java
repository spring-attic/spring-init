package org.springframework.cloud.bootstrap.encrypt;

import java.lang.Override;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import slim.ConditionService;

public class EncryptionBootstrapConfiguration_VanillaEncryptionConfigurationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
  @Override
  public void initialize(GenericApplicationContext context) {
    ConditionService conditions = context.getBeanFactory().getBean(ConditionService.class);
    if (conditions.matches(EncryptionBootstrapConfiguration.VanillaEncryptionConfiguration.class)) {
      if (context.getBeanFactory().getBeanNamesForType(EncryptionBootstrapConfiguration.VanillaEncryptionConfiguration.class).length==0) {
        context.registerBean(EncryptionBootstrapConfiguration.VanillaEncryptionConfiguration.class, () -> new EncryptionBootstrapConfiguration.VanillaEncryptionConfiguration());
        if (conditions.matches(EncryptionBootstrapConfiguration.VanillaEncryptionConfiguration.class, TextEncryptor.class)) {
          context.registerBean("textEncryptor", TextEncryptor.class, () -> context.getBean(EncryptionBootstrapConfiguration.VanillaEncryptionConfiguration.class).textEncryptor());
        }
      }
    }
  }
}
