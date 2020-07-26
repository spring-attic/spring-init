package org.springframework.init.tools.cond;

import java.lang.Override;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.InfrastructureUtils;

public class SampleConfigurationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
  @Override
  public void initialize(GenericApplicationContext context) {
    ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
    if (conditions.matches(SampleConfiguration.class)) {
      if (context.getBeanFactory().getBeanNamesForType(SampleConfiguration.class).length==0) {
        context.registerBean(SampleConfiguration.class, () -> new SampleConfiguration());
        context.registerBean("foo", Foo.class, () -> context.getBean(SampleConfiguration.class).foo());
      }
    }
  }
}