package app.main;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.reactive.RouterFunctionAutoConfigurationInitializer;
import org.springframework.boot.autoconfigure.web.reactive.TomcatReactiveWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfigurationInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPid;
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
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import reactor.core.publisher.Mono;

@SpringBootConfiguration
@ComponentScan
public class SampleApplication {

	@Bean
	public RouterFunction<ServerResponse> userEndpoints() {
		return route().GET("/", req -> ok().body(Mono.just("Hello"), String.class)).build();
	}

	public static void main(String[] args) {
		System.setProperty("io.netty.processId", new ApplicationPid().toString());
		SpringApplication app = new SpringApplicationBuilder(SampleApplication.class)
				.initializers(new InfrastructureInitializer(new Initializer())).build();
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
		if (type == TomcatReactiveWebServerFactoryCustomizer.class) {
			return false;
		}
		if (type == ForwardedHeaderTransformer.class) {
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
				new ReactiveWebServerFactoryAutoConfigurationInitializer(), //
				new RouterFunctionAutoConfigurationInitializer(), //
				new ErrorWebFluxAutoConfigurationInitializer(), //
				new HttpHandlerAutoConfigurationInitializer()));
	}

}
