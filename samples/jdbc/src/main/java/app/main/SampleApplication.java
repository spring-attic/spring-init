package app.main;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.reactive.function.server.RouterFunction;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication(proxyBeanMethods = false)
public class SampleApplication {

	private CustomerRepository foos;

	public SampleApplication(CustomerRepository foos) {
		this.foos = foos;
	}

	@Bean
	public CommandLineRunner runner(ConfigurableListableBeanFactory beans) {
		return args -> {
			Optional<Foo> foo = foos.findById(1L);
			if (!foo.isPresent()) {
				foos.save(new Foo("Hello"));
			}
			System.err.println("Class count: " + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
			System.err.println("Bean count: " + beans.getBeanDefinitionNames().length);
			System.err.println("Bean names: " + Arrays.asList(beans.getBeanDefinitionNames()));
		};
	}

	@Bean
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"),
				request -> ok().body(
						Mono.fromCallable(() -> foos.findById(1L).get()).subscribeOn(Schedulers.boundedElastic()),
						Foo.class));
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

}

interface CustomerRepository extends CrudRepository<Foo, Long> {

}
