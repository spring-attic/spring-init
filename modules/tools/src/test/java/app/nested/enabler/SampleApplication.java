package app.nested.enabler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import lib.selector.EnableBar;

@SpringBootConfiguration
@Import({ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

	@Configuration
	@EnableBar
	protected static class SampleConfiguration {
	}
}
