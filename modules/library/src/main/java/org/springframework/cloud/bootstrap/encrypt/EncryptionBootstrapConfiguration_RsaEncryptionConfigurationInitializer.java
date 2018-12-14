package org.springframework.cloud.bootstrap.encrypt;

import java.lang.Override;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import slim.ConditionService;
import slim.ImportRegistrars;

public class EncryptionBootstrapConfiguration_RsaEncryptionConfigurationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
  @Override
  public void initialize(GenericApplicationContext context) {
    ConditionService conditions = context.getBeanFactory().getBean(ConditionService.class);
    if (conditions.matches(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class)) {
      if (context.getBeanFactory().getBeanNamesForType(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class).length==0) {
        context.getBeanFactory().getBean(ImportRegistrars.class).add(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class, "org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelector");
        context.registerBean(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class, () -> new EncryptionBootstrapConfiguration.RsaEncryptionConfiguration());
        if (conditions.matches(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class, TextEncryptor.class)) {
          context.registerBean("textEncryptor", TextEncryptor.class, () -> context.getBean(EncryptionBootstrapConfiguration.RsaEncryptionConfiguration.class).textEncryptor());
        }
      }
    }
  }
}
