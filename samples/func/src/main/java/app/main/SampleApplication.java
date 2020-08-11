package app.main;

import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.RouterFunctionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.init.func.InfrastructureInitializer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootConfiguration
@ImportAutoConfiguration({ PropertyPlaceholderAutoConfiguration.class, ConfigurationPropertiesAutoConfiguration.class,
		ReactiveWebServerFactoryAutoConfiguration.class, RouterFunctionAutoConfiguration.class,
		ErrorWebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class })
@ComponentScan
public class SampleApplication {

	@Bean
	public RouterFunction<ServerResponse> userEndpoints() {
		return route().GET("/", req -> ok().body(Mono.just("Hello"), String.class)).build();
	}

	public static void main(String[] args) {
		System.setProperty("io.netty.processId", new ApplicationPid().toString());
		SpringApplication app = new SpringApplicationBuilder(SampleApplication.class)
				.initializers(new InfrastructureInitializer().binding(ServerProperties.class, SampleApplication::bind))
				.build();
		app.run(args);
	}

	private static ServerProperties bind(ServerProperties bean, Environment environment) {
		bean.setPort(environment.getProperty("server.port", Integer.class,
				environment.getProperty("SERVER_PORT", Integer.class, 8080)));
		return bean;
	}

}
