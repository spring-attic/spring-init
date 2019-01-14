package com.example.mixt;

import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.WebFluxConfigurations;
import org.springframework.init.factory.FactorySpringApplication;

@SpringInitApplication(WebFluxConfigurations.class)
public class MixtApplication {

	public static void main(String[] args) {
		FactorySpringApplication.run(MixtApplication.class, args);
	}
}
