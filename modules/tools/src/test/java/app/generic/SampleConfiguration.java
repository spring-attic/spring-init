package app.generic;

import java.util.Arrays;
import java.util.Collection;

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
	public Bar<Foo> bar(Foo foo) {
		return new Bar<>(foo);
	}

	@Bean
	public Bar<Collection<Foo>> bars(Foo foo) {
		return new Bar<>(Arrays.asList(foo));
	}

	@Bean
	public CommandLineRunner runner(Bar<Foo> bar) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar);
		};
	}

	@Bean
	public CommandLineRunner another(Bar<Collection<Foo>> bar) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar);
			System.out.println("Foo: " + bar);
		};
	}

}
