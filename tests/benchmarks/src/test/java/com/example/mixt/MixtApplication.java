package com.example.mixt;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.init.factory.FactorySpringApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class MixtApplication {

	public static void main(String[] args) {
		FactorySpringApplication.run(MixtApplication.class, args);
	}

}
