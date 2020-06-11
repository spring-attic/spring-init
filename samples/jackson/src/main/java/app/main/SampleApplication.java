package app.main;

import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication(proxyBeanMethods = false)
public class SampleApplication {

	@Bean
	public Foo foo() {
		return new Foo();
	}

	@Bean
	public RouterFunction<?> userEndpoints(Foo foo) {
		return route(GET("/"), request -> ok().body(Mono.just(foo), Foo.class));
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

}
