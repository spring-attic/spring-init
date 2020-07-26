package app.main;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.annotation.Bean;
import org.springframework.init.func.InfrastructureInitializer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@SpringBootApplication(proxyBeanMethods = false)
public class SampleApplication {

	@Bean
	public RouterFunction<ServerResponse> userEndpoints() {
		return route().GET("/", req -> ok().body(Mono.just("Hello"), String.class)).build();
	}

	public static void main(String[] args) {
		System.setProperty("io.netty.processId", new ApplicationPid().toString());
		SpringApplication app = new SpringApplicationBuilder(SampleApplication.class)
				.initializers(new InfrastructureInitializer()).build();
		app.run(args);
	}

}
