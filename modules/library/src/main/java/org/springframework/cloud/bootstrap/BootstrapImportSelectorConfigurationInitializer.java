package org.springframework.cloud.bootstrap;

import java.lang.Override;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import slim.ImportRegistrars;

public class BootstrapImportSelectorConfigurationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
  @Override
  public void initialize(GenericApplicationContext context) {
    if (context.getBeanFactory().getBeanNamesForType(BootstrapImportSelectorConfiguration.class).length==0) {
      context.getBeanFactory().getBean(ImportRegistrars.class).add(BootstrapImportSelectorConfiguration.class, "org.springframework.cloud.bootstrap.BootstrapImportSelector");
      context.registerBean(BootstrapImportSelectorConfiguration.class, () -> new BootstrapImportSelectorConfiguration());
    }
  }
}
