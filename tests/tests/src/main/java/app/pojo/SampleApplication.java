package app.pojo;

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
@Import({ SampleConfiguration.class, Foo.class, 
		ConfigurationPropertiesAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

	@Configuration
	@Import(FooCommandLineRunner.class)
	protected static class FooCommandConfiguration {
	}
	
	protected static class FooCommandLineRunner implements CommandLineRunner {
		private Bar bar;
		@Value("${app.value}")
		private String message;

		public FooCommandLineRunner(Bar bar) {
			this.bar = bar;
		}

		public void run(String... args) throws Exception {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar.getFoo());
		};
	}

}

@Configuration
class SampleConfiguration {

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

}