package app.sttc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleConfiguration {

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@Bean
	public static Foo foo() {
		return new Foo("foo");
	}

}