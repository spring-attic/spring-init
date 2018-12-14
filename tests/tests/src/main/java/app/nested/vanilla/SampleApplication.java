package app.nested.vanilla;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import app.nested.vanilla.SampleApplication.SampleRegistrar;

@SpringBootConfiguration
@Import({ConfigurationPropertiesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class, SampleRegistrar.class })
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

	@Configuration
	protected static class SampleConfiguration {
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

	}
	
	protected static class SampleRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			registry.registerBeanDefinition("internalBarConfiguration",
					BeanDefinitionBuilder.genericBeanDefinition(BarPostProcessor.class).getBeanDefinition());
		}

		private static class BarPostProcessor implements BeanDefinitionRegistryPostProcessor {

			private ConfigurableListableBeanFactory context;

			public CommandLineRunner runner(Bar bar) {
				return args -> {
					System.out.println("Bar: " + bar);
					System.out.println("Foo: " + bar.getFoo());
				};
			}

			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory context)
					throws BeansException {
				this.context = context;
			}

			@Override
			public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
					throws BeansException {
				registry.registerBeanDefinition("runner",
						BeanDefinitionBuilder
								.genericBeanDefinition(CommandLineRunner.class,
										() -> runner(context.getBean(Bar.class)))
								.getBeanDefinition());
			}
		}
	}
}
