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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.init.tools.cond.ConditionalApplication;
import org.springframework.util.ClassUtils;

import com.squareup.javapoet.JavaFile;

public class SimpleProcessorTests {

	@Test
	public void simpleConditional() {
		Set<JavaFile> files = new InitializerClassProcessor().process(ConditionalApplication.class);
		assertThat(files).hasSize(2);
	}

	@Test
	public void collection() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("collection"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("runner(context.getBeanProvider(Bar.class)))");
	}

	@Test
	public void generic() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("generic"));
		assertThat(files).hasSize(2);
		// System.err.println(files);
		assertThat(files.toString()).contains("bar(context.getBean(Foo.class))");
		assertThat(files.toString()).contains("runner(context.getBean(Bar.class))");
		// TODO: need to use ObjectProvider to wire a Bar<Collection<Foo>> correctly
	}

	@Test
	public void conditionalBean() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("condition.bean"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("ConditionService conditions = InfrastructureUtils.getBean(");
		assertThat(files.toString()).contains("conditions.matches(SampleConfiguration.class, Bar.class)");
	}

	@Test
	public void providerOfGeneric() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.generic"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("ResolvableType.forClassWithGenerics(Bar.class, Foo.class)");
	}

	@Test
	public void providerOfCollectionOfGeneric() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.collection"));
		assertThat(files).hasSize(2);
		// System.err.println(files);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("ResolvableType.forClassWithGenerics(Collection.class, Bar.class)");
	}

	@Test
	public void providerOfMultiGeneric() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.multi"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("ResolvableType.forClassWithGenerics(Bar.class, Foo.class, Foo.class)");
	}

	@Test
	public void providerOfArray() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.array"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains(
				"runner(context.getBeanProvider(Bar.class).stream().collect(Collectors.toList()).toArray(new Bar[0]))");
		assertThat(files.toString()).contains("bar(ObjectUtils.array(context, Foo.class))");
	}

	@Test
	public void providerOfMap() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("provider.map"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("runner(ObjectUtils.map(context, Bar.class))");
	}

	@Test
	public void map() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("map"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("bar(context.getBeansOfType(Foo.class))");
		assertThat(files.toString()).contains("runner(context.getBeanProvider(Bar.class))");
	}

	@Test
	public void list() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("list"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("runner(context.getBeanProvider(Bar.class)))");
		assertThat(files.toString())
				.contains("bar(context.getBeanProvider(Foo.class).stream().collect(Collectors.toList()))");
	}

	@Test
	public void notVisibleComponent() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("vsble"));
		assertThat(files).hasSize(1);
		assertThat(files.toString())
				.contains("ClassUtils.resolveClassName(\"app.vsble.sub.Runner\", context.getClassLoader())");
		assertThat(files.toString()).contains(
				"context.getBean(SampleApplication.class).bar(context.getBean(Foo.class)), def -> def.setInitMethodName(\"start\")");
	}

	@Test
	public void scanSubPackage() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("scan.sub"));
		assertThat(files).hasSize(2);
		assertThat(files.toString()).contains("new SampleConfigurationInitializer().initialize(context)");
		assertThat(files.toString()).contains("context.getBean(SampleConfiguration.class).foo()");
		assertThat(files.toString()).contains(
				".add(SampleApplication.class, \"org.springframework.boot.autoconfigure.AutoConfigurationPackages.Registrar\")");
	}

	@Test
	public void scanOtherPackage() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("scan.other"));
		assertThat(files).hasSize(2);
		// System.err.println(files);
		assertThat(files.toString()).contains("new SampleConfigurationInitializer().initialize(context)");
	}

	@Test
	public void servlet() {
		Set<JavaFile> files = new InitializerClassProcessor().process(app("servlet"));
		assertThat(files).hasSize(1);
		// System.err.println(files);
		assertThat(files.toString()).contains("InfrastructureUtils.invokeAwareMethods(new ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar()");
		assertThat(files.toString()).contains(".registerBeanDefinitions(null, context)");
	}

	private Class<?> app(String pkg) {
		return ClassUtils.resolveClassName("app." + pkg + ".SampleApplication", null);
	}

}
