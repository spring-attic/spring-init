package com.example.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class InitApplication {

	public static void main(String[] args) {
		new SpringApplication(InitApplication.class).run(args);
	}

}
