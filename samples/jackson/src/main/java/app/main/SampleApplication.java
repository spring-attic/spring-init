package app.main;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.JacksonConfigurations;
import org.springframework.init.config.WebFluxConfigurations;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Mono;

@SpringInitApplication({ JacksonConfigurations.class, WebFluxConfigurations.class })
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
