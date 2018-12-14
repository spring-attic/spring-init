package lib.sub;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BarConfiguration {

	@Bean
	public Foo foo() {
		return new Foo();
	}

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

}
