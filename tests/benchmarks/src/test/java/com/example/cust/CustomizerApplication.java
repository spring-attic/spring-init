package com.example.cust;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.BackgroundPreinitializer;
import org.springframework.boot.context.ContextIdApplicationContextInitializer;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.BeanCountingApplicationListener;
import org.springframework.init.config.StartupApplicationListener;
import org.springframework.init.config.WebFluxConfigurations;
import org.springframework.init.factory.FactorySpringApplication;
import org.springframework.init.factory.SpringApplicationCustomizer;
import org.springframework.init.factory.SpringApplicationCustomizers;

@SpringInitApplication(WebFluxConfigurations.class)
@SpringApplicationCustomizers(FactoriesApplicationCustomizer.class)
public class CustomizerApplication {

	public static void main(String[] args) {
		SpringApplication application = new FactorySpringApplication(
				CustomizerApplication.class);
		application.run(args);
		System.out.println("Benchmark app started");
	}
}

class FactoriesApplicationCustomizer implements SpringApplicationCustomizer {

	@Override
	public void customize(SpringApplication application, String[] args) {
		application.setListeners(Arrays.asList(new BackgroundPreinitializer(),
				new ConfigFileApplicationListener(), new LoggingApplicationListener(),
				new BeanCountingApplicationListener(), new StartupApplicationListener()));
		application.setInitializers(
				Arrays.asList(new ContextIdApplicationContextInitializer()));
	}

}