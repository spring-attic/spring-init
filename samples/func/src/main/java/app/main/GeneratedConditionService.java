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
    TYPES.put("org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryConfiguration$EmbeddedNetty", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration$ResourceChainCustomizerConfiguration", false);
    TYPES.put("org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration", true);
    TYPES.put("org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration", true);
  }
  static {
    METHODS.put("org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryConfiguration$EmbeddedNetty", new HashSet<>(Arrays.asList("org.springframework.http.client.reactive.ReactorResourceFactory")));
    METHODS.put("org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration", new HashSet<>(Arrays.asList("org.springframework.context.support.PropertySourcesPlaceholderConfigurer")));
    METHODS.put("org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration", new HashSet<>(Arrays.asList("org.springframework.boot.web.reactive.error.DefaultErrorAttributes")));
    METHODS.put("org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration", new HashSet<>());
    METHODS.put("org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration", new HashSet<>());
  }

  public GeneratedConditionService() {
    super(TYPES, METHODS);
  }
}
