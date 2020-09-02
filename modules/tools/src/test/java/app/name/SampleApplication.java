package app.name;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class SampleApplication {

	@Bean(name="barBean")
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@Bean(name="fooBean")
	public Foo bar() {
		return new Foo("foo");
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}
