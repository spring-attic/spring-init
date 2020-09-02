/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.init.tools;

import java.util.Set;

import com.squareup.javapoet.JavaFile;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.init.tools.cond.ConditionalApplication;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleProcessorTests {

	@Test
	public void managementContext() {
		Set<JavaFile> files = new InitializerClassProcessor().process(ManagementContextAutoConfiguration.class);
		// System.err.println(files);
		assertThat(files).hasSize(4);
		assertThat(files.toString()).contains(
				"new ManagementContextAutoConfiguration_SameManagementContextConfigurationInitializer().initialize(context)");
	}

	@Test
	public void simpleConditional() {
		Set<JavaFile> files = new InitializerClassProcessor().process(ConditionalApplication.class);
		assertThat(files).hasSize(3);
		assertThat(files.toString()).contains("AutoConfigurationPackages.register(");
	}

	@Test
	public void conditionalOnMissingType() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("condition.type"));
		assertThat(files).hasSize(5);
		// System.err.println(files);
		assertThat(files.toString()).contains("context.getBean(SampleConfiguration.class).foo()");
		assertThat(files.toString()).doesNotContain("\"not.going.to.be.There\"");
	}

	@Test
	public void conditionalOnPresentType() {
		InitializerClassProcessor processor = new InitializerClassProcessor();
		Set<JavaFile> files = processor.process(app("condition.present"));
		assertThat(files).hasSize(6);
		// System.err.println(files);
		assertThat(files.toString()).contains("context.getBean(SampleConfiguration.class).foo()");
		assertThat(files.toString()).contains("ClassUtils.isPresent(\"java.lang.String\", null)");
		assertThat(processor.getBuildTimes()).contains("app.condition.present.ConditionalConfigurationInitializer");
	}

	@Test
	public void resourceXmlImport() {
		InitializerClassProcessor processor = new InitializerClassProcessor();
		Set<JavaFile> files = processor.process(app("resource"));
		assertThat(files).hasSize(4);
		// System.err.println(files);
		assertThat(files.toString()).contains("!context.getEnvironment().getProperty(");
		assertThat(files.toString()).contains("new XmlInitializer(\"bar-config.xml\")");
		assertThat(files.toString()).contains("spring.xml.ignore");
	}

	@Test
	public void conditionalOnComponent() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("condition.component"));
		assertThat(files).hasSize(6);
		// System.err.println(files);
		assertThat(files.toString()).contains("conditions.matches(Foo.class)");
	}

	@Test
	public void resource() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("resource"));
		assertThat(files).hasSize(4);
		assertThat(files.toString()).contains("new XmlInitializer(\"bar-config.xml\").initialize(context)");
	}

	@Test
	public void collection() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("collection"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("runner(context.getBeanProvider(Bar.class)))");
	}

	@Test
	public void generic() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("generic"));
		assertThat(files).hasSize(5);
		// System.err.println(files);
		assertThat(files.toString()).contains("bar(context.getBean(Foo.class))");
		assertThat(files.toString()).contains("runner(context.getBean(Bar.class))");
		// TODO: need to use ObjectProvider to wire a Bar<Collection<Foo>> correctly
	}

	@Test
	public void conditionalBean() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("condition.bean"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("ConditionService conditions = InfrastructureUtils.getBean(");
		assertThat(files.toString()).contains("conditions.matches(SampleConfiguration.class, Bar.class)");
	}

	@Test
	public void providerOfGeneric() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.generic"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("ResolvableType.forClassWithGenerics(Bar.class, Foo.class)");
	}

	@Test
	public void providerOfCollectionOfGeneric() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.collection"));
		assertThat(files).hasSize(5);
		// System.err.println(files);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("ResolvableType.forClassWithGenerics(Collection.class, Bar.class)");
	}

	@Test
	public void providerOfMultiGeneric() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.multi"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("ResolvableType.forClassWithGenerics(Bar.class, Foo.class, Foo.class)");
	}

	@Test
	public void providerOfArray() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.array"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains(
				"runner(context.getBeanProvider(Bar.class).stream().collect(Collectors.toList()).toArray(new Bar[0]))");
		assertThat(files.toString()).contains("bar(ObjectUtils.array(context, Foo.class))");
	}

	@Test
	public void providerOfMap() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.map"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("runner(ObjectUtils.map(context, Bar.class))");
	}

	@Test
	public void map() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("map"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("runner(context.getBeanProvider(Bar.class))");
	}

	@Test
	public void list() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("list"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("runner(context.getBeanProvider(Bar.class)))");
		assertThat(files.toString())
				.contains("bar(context.getBeanProvider(Foo.class).stream().collect(Collectors.toList()))");
	}

	@Test
	public void notVisibleComponent() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("vsble"));
		assertThat(files).hasSize(4);
		assertThat(files.toString()).contains("types = ");
		assertThat(files.toString()).contains("types.getType(\"app.vsble.sub.Runner\")");
		assertThat(files.toString()).contains(
				"context.getBean(SampleApplication.class).bar(context.getBean(Foo.class)), def -> def.setInitMethodName(\"start\")");
	}

	@Test
	public void scanSubPackage() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("scan.sub"));
		assertThat(files).hasSize(3);
		assertThat(files.toString()).doesNotContain("types = ");
		assertThat(files.toString()).contains("new SampleConfigurationInitializer().initialize(context)");
		assertThat(files.toString()).contains("context.getBean(SampleConfiguration.class).foo()");
		assertThat(files.toString()).contains("AutoConfigurationPackages.register(context, \"app.scan.sub\")");
	}

	@Test
	public void selector() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("selector"));
		assertThat(files).hasSize(4);
		assertThat(files.toString()).contains(
				"InfrastructureUtils.getBean(context.getBeanFactory(), ImportRegistrars.class).add(SampleApplication.class, SampleRegistrar.class)");
		assertThat(files.toString())
				.contains("new PropertyPlaceholderAutoConfigurationInitializer().initialize(context)");
	}

	@Test
	public void scanOtherPackage() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("scan.other"));
		assertThat(files).hasSize(5);
		assertThat(files.toString()).contains("new SampleConfigurationInitializer().initialize(context)");
	}

	@Test
	public void servlet() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("servlet"));
		assertThat(files).hasSize(4);
		assertThat(files.toString()).contains(
				"InfrastructureUtils.invokeAwareMethods(new ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar()");
		assertThat(files.toString()).contains(".registerBeanDefinitions(null, context)");
	}

	@Test
	public void namedBean() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("name"));
		// System.err.println(files);
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("context.registerBean(\"fooBean\"");
		assertThat(files.toString()).contains("context.registerBean(\"barBean\"");
	}

	@Test
	public void overrideBean() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("override"));
		// System.err.println(files);
		assertThat(files).hasSize(2);
		assertThat(files.toString()).containsOnlyOnce("context.registerBean(\"foo\"");
		assertThat(files.toString()).containsOnlyOnce("context.registerBean(\"bar\"");
	}

	private Class<?> app(String pkg) {
		return ClassUtils.resolveClassName("app." + pkg + ".SampleApplication", null);
	}

}
