package com.example.none;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.BackgroundPreinitializer;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.BeanCountingApplicationListener;
import org.springframework.init.config.WebFluxConfigurations;

@SpringInitApplication(WebFluxConfigurations.class)
public class NoneApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(NoneApplication.class);
		application.setInitializers(Collections.emptyList());
		application.setListeners(Arrays.asList(new BackgroundPreinitializer(),
				new BeanCountingApplicationListener()));
		application.run(args);
		System.out.println("Benchmark app started");
	}
}