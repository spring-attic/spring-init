package app.statc.abstrct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

abstract class AbstractSampleConfiguration {

	@Configuration
	@ConditionalOnProperty(prefix="app", name="enabled", havingValue="true", matchIfMissing=true)
	static class SampleConfiguration {

		@Value("${app.value}")
		private String message;

		@Bean
		public Foo foo() {
			return new Foo();
		}

		@Bean
		public Bar bar(Foo foo) {
			return new Bar(foo);
		}

		@Bean
		public CommandLineRunner runner(Bar bar) {
			return args -> {
				System.out.println("Message: " + message);
				System.out.println("Bar: " + bar);
				System.out.println("Foo: " + bar.getFoo());
			};
		}
		
		@SuppressWarnings("unused")
		private class SecretClass {
			
		}

	}
}

