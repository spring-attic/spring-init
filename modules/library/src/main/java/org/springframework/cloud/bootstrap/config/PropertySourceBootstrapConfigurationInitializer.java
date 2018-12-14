package org.springframework.cloud.bootstrap.config;

import java.lang.Override;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import slim.ImportRegistrars;

public class PropertySourceBootstrapConfigurationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
  @Override
  public void initialize(GenericApplicationContext context) {
    if (context.getBeanFactory().getBeanNamesForType(PropertySourceBootstrapConfiguration.class).length==0) {
      context.getBeanFactory().getBean(ImportRegistrars.class).add(PropertySourceBootstrapConfiguration.class, "org.springframework.boot.context.properties.EnableConfigurationPropertiesImportSelector");
      context.registerBean(PropertySourceBootstrapConfiguration.class, () -> new PropertySourceBootstrapConfiguration());
    }
  }
}
