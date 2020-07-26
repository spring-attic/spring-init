package org.springframework.init.tools.bean;

import java.lang.Override;
import org.springframework.boot.autoconfigure.AutoConfigurationImportSelector;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;

public class ConditionalBeanApplicationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
  @Override
  public void initialize(GenericApplicationContext context) {
    if (context.getBeanFactory().getBeanNamesForType(ConditionalBeanApplication.class).length==0) {
      ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
      if (conditions.includes(SampleConfiguration.class)) {
        new SampleConfigurationInitializer().initialize(context);
      }
      context.registerBean(ConditionalBeanApplication.class, () -> new ConditionalBeanApplication());
      InfrastructureUtils.getBean(context.getBeanFactory(), ImportRegistrars.class).add(ConditionalBeanApplication.class, AutoConfigurationImportSelector.class);
      AutoConfigurationPackages.register(context, "org.springframework.init.tools.cond");
    }
  }
}