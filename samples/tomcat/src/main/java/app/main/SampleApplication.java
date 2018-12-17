package app.main;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.init.SpringInitApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringInitApplication({ PropertyPlaceholderAutoConfiguration.class,
		ConfigurationPropertiesAutoConfiguration.class,
		ServletWebServerFactoryAutoConfiguration.class, WebMvcAutoConfiguration.class,
		ErrorMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class })
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}
}

@Component
class Runner implements CommandLineRunner {

	private ConfigurableListableBeanFactory beans;

	public Runner(ConfigurableListableBeanFactory beans) {
		this.beans = beans;
	}

	@Override
	public void run(String... args) throws Exception {

		System.err.println("Class count: "
				+ ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
		System.err.println("Bean count: " + beans.getBeanDefinitionNames().length);
		System.err
				.println("Bean names: " + Arrays.asList(beans.getBeanDefinitionNames()));
	}
}

@RestController
class SampleController {

	private Foo foo;

	public SampleController(Foo foo) {
		this.foo = foo;
	}

	@GetMapping("/")
	String home() {
		return foo.getValue();
	}

}

@Component
class Foo {

	@Value("${app.value}")
	private String value;

	public Foo() {
	}

	public Foo(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Foo [value=" + this.value + "]";
	}

}
