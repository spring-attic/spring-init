package app.enable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Import;

import lib.enable.EnableBar;

@SpringBootConfiguration
@EnableBar
// TODO: make this work with `@Import(ConfigurationPropertiesAutoConfiguration.class)`
@Import({ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class})
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.setLogStartupInfo(false);
		app.run(args);
	}

}
