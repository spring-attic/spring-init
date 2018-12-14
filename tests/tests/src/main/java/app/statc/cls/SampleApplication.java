package app.statc.cls;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@Import({ ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

	@Configuration
	@ConditionalOnProperty(prefix="app", name="enabled", havingValue="true", matchIfMissing=true)
	static class SampleConfiguration {

		@Value("${app.value}")
		private String message;

		@Bean
		public Foo foo() {
			return new Foo();
		}

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
		
		@SuppressWarnings("unused")
		private class SecretClass {
			
		}

	}
}

