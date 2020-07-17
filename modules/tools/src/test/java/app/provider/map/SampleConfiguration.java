package app.provider.map;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleConfiguration {

	@Value("${app.value}")
	private String message;

	@Bean
	public Foo foo() {
		return new Foo();
	}

	@Bean
	public Bar bar(Map<String, Foo> foo) {
		return new Bar(foo.values().iterator().next());
	}

	@Bean
	// see https://jira.spring.io/browse/SPR-17577
	public CommandLineRunner runner(ObjectProvider<Map<String, Bar>> bar) {
		return args -> {
			System.out.println("Message: " + message);
			bar.getIfAvailable().forEach((name, value) -> System.out.println("Bar: " + value));
			bar.getIfAvailable().forEach((name, value) -> System.out
					.println("Foo: " + name + "=" + value.getFoo()));
		};
	}

}
