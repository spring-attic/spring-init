package app.main;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

import org.springframework.boot.NativePropertiesListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.embedded.JettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.UndertowWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.servlet.RouterFunctionAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfigurationInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.filter.OrderedHiddenHttpMethodFilter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.ConfigurationSource;
import org.springframework.init.func.InfrastructureInitializer;
import org.springframework.init.func.SimpleConfigurationSource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@SpringBootConfiguration
@ComponentScan
public class SampleApplication {

	@Bean
	public RouterFunction<ServerResponse> userEndpoints() {
		return route().GET("/", req -> ok().body("Hello")).build();
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplicationBuilder(SampleApplication.class)
				.initializers(new InfrastructureInitializer(new Initializer()))
				.listeners(new NativePropertiesListener()).build();
		app.run(args);
	}

}

class SampleConditionService implements ConditionService {

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		if (type.getName().equals(
				"org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration$ResourceChainCustomizerConfiguration")) {
			return false;
		}
		return true;
	}

	@Override
	public boolean matches(Class<?> type) {
		return matches(type, ConfigurationPhase.REGISTER_BEAN);
	}

	@Override
	public boolean matches(Class<?> factory, Class<?> type) {
		System.err.println("Matches: " + factory.getName() + ", " + type.getName());
		if (type == UndertowWebServerFactoryCustomizer.class || type == JettyWebServerFactoryCustomizer.class) {
			return false;
		}
		if (type == ForwardedHeaderTransformer.class) {
			return false;
		}
		if (type == MultipartResolver.class) {
			return false;
		}
		if (type == OrderedHiddenHttpMethodFilter.class) {
			return false;
		}
		return true;
	}

	@Override
	public boolean includes(Class<?> type) {
		return false;
	}

}

class Initializer implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		context.registerBean(ConditionService.class, () -> new SampleConditionService());
		context.registerBean(ConfigurationSource.class, () -> new SimpleConfigurationSource( //
				new SampleApplicationInitializer(), //
				new PropertyPlaceholderAutoConfigurationInitializer(), //
				new ConfigurationPropertiesAutoConfigurationInitializer(), //
				new ServletWebServerFactoryAutoConfigurationInitializer(), //
				new DispatcherServletAutoConfigurationInitializer(), //
				new RouterFunctionAutoConfigurationInitializer(), //
				new ErrorMvcAutoConfigurationInitializer() //
		));
	}

}
