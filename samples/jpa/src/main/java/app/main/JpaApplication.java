package app.main;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.JpaDataConfigurations;
import org.springframework.init.config.WebFluxConfigurations;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@SpringInitApplication({ JpaDataConfigurations.class, WebFluxConfigurations.class,
		JacksonAutoConfiguration.class })
@EntityScan
public class JpaApplication {

	@Autowired
	private FooRepository foos;

	@Bean
	public CommandLineRunner runner(ConfigurableListableBeanFactory beans) {
		return args -> {
			foos.findById(1L).orElseGet(() -> foos.save(new Foo("Hello")));
			System.err.println("Class count: " + ManagementFactory.getClassLoadingMXBean()
					.getTotalLoadedClassCount());
			System.err.println("Bean count: " + beans.getBeanDefinitionNames().length);
			System.err.println(
					"Bean names: " + Arrays.asList(beans.getBeanDefinitionNames()));
		};
	}

	@Bean
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"),
				request -> ok().body(Mono.fromCallable(() -> foos.findById(1L).get())
						.subscribeOn(Schedulers.elastic()), Foo.class));
	}

	public static void main(String[] args) {
		SpringApplication.run(JpaApplication.class, args);
	}

}

interface FooRepository extends CrudRepository<Foo, Long> {
}
