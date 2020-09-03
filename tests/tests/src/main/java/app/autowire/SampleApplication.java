package app.autowire;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@SpringBootConfiguration
@Import({ SampleConfiguration.class, ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {

	@Value("${app.value}")
	private String message;
	private Bar bar;

	@Autowired
	public void bar(Foo foo) {
		this.bar = new Bar(foo);
	}

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar.getFoo());
		};
	}

	public static void main(String[] args) {
		// System.setProperty("spring.functional.enabled", "false");
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
		return new Foo(message);
	}

}
