package app.condition.bean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@Import({ SampleConfiguration.class, ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}

@Configuration
class SampleConfiguration {

	@Value("${app.value}")
	private String message;

	@Bean
	public Foo foo() {
		return new Foo();
	}

	@ConditionalOnClass(name = "not.going.to.be.There")
	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@ConditionalOnClass(name = "not.going.to.be.There")
	@Bean
	public CommandLineRunner runner(Bar bar) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar.getFoo());
		};
	}

}