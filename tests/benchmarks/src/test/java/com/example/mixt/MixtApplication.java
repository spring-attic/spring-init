package com.example.mixt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class MixtApplication {

	public static void main(String[] args) {
		SpringApplication.run(MixtApplication.class, args);
	}

}
