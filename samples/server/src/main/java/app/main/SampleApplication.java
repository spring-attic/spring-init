package app.main;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.BackgroundPreinitializer;
import org.springframework.boot.context.ContextIdApplicationContextInitializer;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.WebFluxConfigurations;
import org.springframework.init.factory.FactorySpringApplication;
import org.springframework.init.factory.SpringApplicationCustomizer;
import org.springframework.init.factory.SpringApplicationCustomizers;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Mono;

@SpringInitApplication(WebFluxConfigurations.class)
@SpringApplicationCustomizers(SimpleFactories.class)
public class SampleApplication {

	@Value("${app.value}")
	private String value;

	@Bean
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"), request -> ok().body(Mono.just(value), String.class));
	}

	@Bean
	public CommandLineRunner runner(ConfigurableListableBeanFactory beans) {
		return args -> {
			System.err.println("Class count: " + ManagementFactory.getClassLoadingMXBean()
					.getTotalLoadedClassCount());
			System.err.println("Bean count: " + beans.getBeanDefinitionNames().length);
			System.err.println(
					"Bean names: " + Arrays.asList(beans.getBeanDefinitionNames()));
		};
	}

	public static void main(String[] args) {
		FactorySpringApplication.run(SampleApplication.class, args);
	}
}

class SimpleFactories implements SpringApplicationCustomizer {

	@Override
	public void customize(SpringApplication application, String[] args) {
		application.setListeners(Arrays.asList(new BackgroundPreinitializer(),
				new ConfigFileApplicationListener(), new LoggingApplicationListener()));
		application.setInitializers(
				Arrays.asList(new ContextIdApplicationContextInitializer()));
	}

}
