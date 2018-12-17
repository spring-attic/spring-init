package app.manual;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@Import(SampleConfiguration.class)
@ImportAutoConfiguration({ ConfigurationPropertiesAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}

@Configuration
class SampleConfiguration {

	@Bean
	public Foo foo() {
		return new Foo();
	}

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@Bean
	public CommandLineRunner runner(Bar bar, @Value("${app.value}") String message) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar.getFoo());
		};
	}

}

// Uncomment this and you get a compiler error. A manual implementation of the initializer
// should be able to override the generated one.
// @formatter:off

/*class SampleConfigurationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		context.registerBean(Foo.class, () -> new Foo("Init"));
		context.registerBean(Bar.class, () -> new Bar(context.getBean(Foo.class)));
		context.registerBean(CommandLineRunner.class, () -> args -> {
			String message = context.getEnvironment().getProperty("app.value");
			Bar bar = context.getBean(Bar.class);
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar.getFoo());
		});
	}

}*/

// @formatter:on