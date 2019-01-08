package app.main;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.JacksonConfigurations;
import org.springframework.init.config.ReactiveMongoDataConfigurations;
import org.springframework.init.config.WebFluxConfigurations;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * You need to start Mongo locally to run the app (e.g. use the docker-compose.yml
 * provided). Starts up with embedded Mongo in tests.
 * 
 * @author Dave Syer
 *
 */
@SpringInitApplication({ ReactiveMongoDataConfigurations.class,
		JacksonConfigurations.class, WebFluxConfigurations.class })
@EntityScan
public class SampleApplication {

	private CustomerRepository foos;

	public SampleApplication(CustomerRepository foos) {
		this.foos = foos;
	}

	@Bean
	public CommandLineRunner runner(ConfigurableListableBeanFactory beans) {
		return args -> {
			foos.findAll().switchIfEmpty(foos.save(new Foo("Hello"))).blockFirst();
			System.err.println("Class count: " + ManagementFactory.getClassLoadingMXBean()
					.getTotalLoadedClassCount());
			System.err.println("Bean count: " + beans.getBeanDefinitionNames().length);
			System.err.println(
					"Bean names: " + Arrays.asList(beans.getBeanDefinitionNames()));
		};
	}

	@Bean
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"), request -> ok().body(foos.findAll().single(), Foo.class));
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

}

interface CustomerRepository extends ReactiveCrudRepository<Foo, String> {
}