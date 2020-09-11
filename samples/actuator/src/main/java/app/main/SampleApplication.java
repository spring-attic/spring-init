package app.main;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.availability.AvailabilityProbesAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.reactive.FunctionalReactiveActuatorEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootConfiguration(proxyBeanMethods = false)
@Import({ ActuatorConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
		ConfigurationPropertiesAutoConfiguration.class, ReactiveWebServerFactoryAutoConfiguration.class,
		WebFluxAutoConfiguration.class, ErrorWebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class })
public class SampleApplication {

	@Value("${app.value}")
	private String value = "Hi";

	@Bean
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"), request -> ok().body(Mono.just(value), String.class));
	}

	@Bean
	public CommandLineRunner runner(ConfigurableListableBeanFactory beans) {
		return args -> {
			System.err.println("Class count: " + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
			System.err.println("Bean count: " + beans.getBeanDefinitionNames().length);
			System.err.println("Bean names: " + Arrays.asList(beans.getBeanDefinitionNames()));
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

}

@Configuration(proxyBeanMethods = false)
@Import({ EndpointAutoConfiguration.class, ApplicationAvailabilityAutoConfiguration.class,
		AvailabilityProbesAutoConfiguration.class, HealthContributorAutoConfiguration.class,
		HealthEndpointAutoConfiguration.class, InfoEndpointAutoConfiguration.class,
		ManagementContextAutoConfiguration.class, FunctionalReactiveActuatorEndpointAutoConfiguration.class })
class ActuatorConfiguration {

}
