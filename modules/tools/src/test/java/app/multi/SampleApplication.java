package app.multi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Import;

import lib.multi.BarConfiguration;
import lib.multi.RunnerConfiguration;

@SpringBootConfiguration
// TODO Don't depend on modules that don't exist when we are compiled - will need two
// separate projects if wanting to truly split library vs in-project dependencies
@Import({ BarConfiguration.class, RunnerConfiguration.class,
		ConfigurationPropertiesAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.setLogStartupInfo(false);
		app.run(args);
	}

}
