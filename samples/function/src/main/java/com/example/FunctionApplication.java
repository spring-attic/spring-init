package com.example;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.function.web.flux.ReactorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.init.SpringInitApplication;

@SpringInitApplication({ PropertyPlaceholderAutoConfiguration.class,
		ConfigurationPropertiesAutoConfiguration.class, JacksonAutoConfiguration.class,
		ReactiveWebServerFactoryAutoConfiguration.class, WebFluxAutoConfiguration.class,
		ErrorWebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
		ContextFunctionCatalogAutoConfiguration.class, ReactorAutoConfiguration.class })
public class FunctionApplication {

	@Bean
	public FunctionRegistration<Function<String, String>> uppercase() {
		return new FunctionRegistration<Function<String, String>>(
				value -> value.toUpperCase())
						.type(FunctionType.from(String.class).to(String.class));
	}

	public static void main(String[] args) {
		SpringApplication.run(FunctionApplication.class, args);
	}
}
