package app.main;

import java.lang.Boolean;
import java.lang.String;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.init.func.SimpleConditionService;

public class GeneratedConditionService extends SimpleConditionService {
  private static Map<String, Set<String>> METHODS = new HashMap<>();

  private static Map<String, Boolean> TYPES = new HashMap<>();

  static {
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration$WhitelabelErrorViewConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryConfiguration$EmbeddedTomcat", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletRegistrationConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration$ResourceChainCustomizerConfiguration", false);
  }
  static {
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration$WhitelabelErrorViewConfiguration", new HashSet<>(Arrays.asList("org.springframework.web.servlet.view.BeanNameViewResolver")));
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration", new HashSet<>(Arrays.asList("org.springframework.boot.web.servlet.error.DefaultErrorAttributes")));
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletRegistrationConfiguration", new HashSet<>(Arrays.asList("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean")));
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration$DefaultErrorViewResolverConfiguration", new HashSet<>(Arrays.asList("org.springframework.boot.autoconfigure.web.servlet.error.DefaultErrorViewResolver")));
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration$EnableWebMvcConfiguration", new HashSet<>(Arrays.asList("org.springframework.web.servlet.LocaleResolver")));
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration$DispatcherServletConfiguration", new HashSet<>());
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration", new HashSet<>());
    METHODS.put("org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration", new HashSet<>(Arrays.asList("org.springframework.context.support.PropertySourcesPlaceholderConfigurer")));
    METHODS.put("org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration", new HashSet<>(Arrays.asList("org.springframework.boot.autoconfigure.web.servlet.TomcatServletWebServerFactoryCustomizer")));
  }

  public GeneratedConditionService() {
    super(TYPES, METHODS);
  }
}
