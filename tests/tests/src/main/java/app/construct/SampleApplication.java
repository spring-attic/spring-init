package app.construct;

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
@Import({ ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {

	@Bean
	public Foo foo() {
		return new Foo();
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

	@Configuration
	protected static class SampleConfiguration {
		@Value("${app.value}")
		private String message;
		private Foo foo;

		public SampleConfiguration(Foo foo) {
			this.foo = foo;
		}

		@Bean
		public Bar bar() {
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
	}
}
