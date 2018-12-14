package app.provider.array;

import java.util.Arrays;
import java.util.stream.Collectors;

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
	public Bar bar(ObjectProvider<Foo[]> foo) {
		return new Bar(new Foo(Arrays.asList(foo.getIfUnique()).stream()
				.map(v -> v.getValue()).collect(Collectors.joining())));
	}

	@Bean
	public CommandLineRunner runner(Bar[] bar) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + Arrays.asList(bar).stream()
					.map(v -> v.toString()).collect(Collectors.joining(",")));
			System.out.println("Foo: " + Arrays.asList(bar).stream()
					.map(v -> v.getFoo().toString()).collect(Collectors.joining(",")));
		};
	}

}
