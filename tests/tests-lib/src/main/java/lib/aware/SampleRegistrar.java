package lib.aware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

class SampleRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

	private ResourceLoader resourceLoader;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		Assert.notNull(resourceLoader, "ResourceLoader cannot be null");
		registry.registerBeanDefinition("internalBarConfiguration",
				BeanDefinitionBuilder.genericBeanDefinition(BarPostProcessor.class).getBeanDefinition());
	}

	private static class BarPostProcessor implements BeanDefinitionRegistryPostProcessor {

		private ConfigurableListableBeanFactory context;

		public Foo foo() {
			return new Foo();
		}

		public Bar bar(Foo foo) {
			return new Bar(foo);
		}

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
			registry.registerBeanDefinition("foo", BeanDefinitionBuilder
					.genericBeanDefinition(Foo.class, () -> foo()).getBeanDefinition());
			registry.registerBeanDefinition("bar",
					BeanDefinitionBuilder
							.genericBeanDefinition(Bar.class,
									() -> bar(context.getBean(Foo.class)))
							.getBeanDefinition());
			registry.registerBeanDefinition("runner",
					BeanDefinitionBuilder
							.genericBeanDefinition(CommandLineRunner.class,
									() -> runner(context.getBean(Bar.class)))
							.getBeanDefinition());
		}
	}

}
