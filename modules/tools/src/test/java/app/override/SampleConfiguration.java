package app.override;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleConfiguration {
	
	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@Bean
	public Foo foo() {
		return new Foo("foo");
	}

}