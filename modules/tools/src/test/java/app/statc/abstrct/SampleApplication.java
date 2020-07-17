package app.statc.abstrct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
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
	@Import(AbstractSampleConfiguration.SampleConfiguration.class)
	protected static class SampleConfiguration {
	}
}

