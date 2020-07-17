package app.importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Import;

import lib.imported.Bar;
import lib.importer.SomeConfiguration;

@SpringBootConfiguration
@Import({ SomeConfiguration.class, ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {
	
	@Autowired
	Bar bar;

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.setLogStartupInfo(false);
		app.run(args);
	}

}
