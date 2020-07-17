package app.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import app.order.auto.AutoConfiguration;
import app.order.auto.ManualConfiguration;

@SpringBootConfiguration
@Import(SampleConfiguration.class)
@ImportAutoConfiguration({ ManualConfiguration.class })
public class SampleApplication {

	@Value("${app.value}")
	private String message;

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@Bean
	public CommandLineRunner runner(Bar bar) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar.getFoo());
		};
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}

@Configuration
@ImportAutoConfiguration(AutoConfiguration.class)
class SampleConfiguration {
}