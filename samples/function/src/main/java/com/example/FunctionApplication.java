package com.example;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(proxyBeanMethods = false)
public class FunctionApplication {

	@Bean
	public FunctionRegistration<Function<String, String>> uppercase() {
		return new FunctionRegistration<Function<String, String>>(value -> value.toUpperCase())
				.type(FunctionType.from(String.class).to(String.class));
	}

	public static void main(String[] args) {
		SpringApplication.run(FunctionApplication.class, args);
	}

}
