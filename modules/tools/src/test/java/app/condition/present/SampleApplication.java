package app.condition.present;

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

@SpringBootConfiguration(proxyBeanMethods = false)
@Import({ SampleConfiguration.class, ConditionalConfiguration.class, ConfigurationPropertiesAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}

@Configuration(proxyBeanMethods = false)
class SampleConfiguration {

	@Bean
	public Foo foo() {
		return new Foo();
	}

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

}

@Configuration
@ConditionalOnClass(String.class)
class ConditionalConfiguration {

	@Value("${app.value}")
	private String message;

	@Bean
	public CommandLineRunner runner(Bar bar) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar.getFoo());
		};
	}

}