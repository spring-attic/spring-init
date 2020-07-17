package app.factory;

import java.util.stream.Collectors;

import org.springframework.beans.factory.FactoryBean;
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
	public FactoryBean<Foo> foo() {
		return new FactoryBean<Foo>() {

			@Override
			public Foo getObject() throws Exception {
				return new Foo();
			}

			@Override
			public Class<?> getObjectType() {
				return Foo.class;
			}
		};
	}

	@Bean
	public Bar bar(Foo foo) {
		return new Bar(foo);
	}

	@Bean
	public CommandLineRunner runner(ObjectProvider<Bar> bar) {
		return args -> {
			System.out.println("Message: " + message);
			System.out.println("Bar: " + bar.stream().map(v -> v.toString())
					.collect(Collectors.joining(",")));
			System.out.println("Foo: " + bar.stream().map(v -> v.getFoo().toString())
					.collect(Collectors.joining(",")));
		};
	}

}
