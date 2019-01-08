package app.main;

import org.springframework.boot.SpringApplication;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.BasicConfigurations;

@SpringInitApplication(BasicConfigurations.class)
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}
