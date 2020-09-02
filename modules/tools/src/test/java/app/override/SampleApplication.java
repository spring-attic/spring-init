package app.override;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootConfiguration
public class SampleApplication extends SampleConfiguration {

	@Bean
	@Override
	public Bar bar(Foo foo) {
		return new Bar(new Foo("_" + foo.getValue()));
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}

@Configuration
class SampleConfiguration {
	
	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@Bean
	public Foo foo() {
		return new Foo("foo");
	}

}