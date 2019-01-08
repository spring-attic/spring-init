package com.example.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.reactive.ReactiveManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.init.EnableSelectedAutoConfiguration;
import org.springframework.init.SpringInitApplication;

@SpringInitApplication({ PropertyPlaceholderAutoConfiguration.class,
		ConfigurationPropertiesAutoConfiguration.class,
		ReactiveWebServerFactoryAutoConfiguration.class, WebFluxAutoConfiguration.class,
		ErrorWebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class })
@Import(ActuatorConfiguration.class)
public class InitApplication {

	public static void main(String[] args) {
		new SpringApplication(InitApplication.class).run(args);
	}
}

@Configuration
@ConditionalOnClass(Endpoint.class)
@EnableSelectedAutoConfiguration({ EndpointAutoConfiguration.class,
		HealthIndicatorAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
		InfoEndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
		ReactiveManagementContextAutoConfiguration.class,
		ManagementContextAutoConfiguration.class })
class ActuatorConfiguration {

}