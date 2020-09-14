package app.variable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleConfiguration {

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@SuppressWarnings("unchecked")
	@Bean
	public <T extends Foo> T foo() {
		return (T) new Foo("foo");
	}

}