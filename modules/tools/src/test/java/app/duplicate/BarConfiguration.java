package app.duplicate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BarConfiguration {

	@Configuration
	public static class Config {

		@Bean
		public Bar bar(Foo foo) {
			return new Bar(foo);
		}

	}

}
