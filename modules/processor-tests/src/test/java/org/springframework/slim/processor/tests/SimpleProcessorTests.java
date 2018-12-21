/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.slim.processor.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.function.compiler.java.CompilationMessage;
import org.springframework.cloud.function.compiler.java.CompilationResult;
import org.springframework.cloud.function.compiler.java.DependencyResolver;
import org.springframework.cloud.function.compiler.java.InputFileDescriptor;
import org.springframework.core.io.FileUrlResource;
import org.springframework.slim.processor.infra.CompilerRunner;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Simple tests around processor invocation.
 * 
 * @author Andy Clement
 */
public class SimpleProcessorTests {

	/**
	 * Check for an expected message that indicates the processor ran
	 */
	@Test
	public void processorInvoked() {
		CompilationResult cr = CompilerRunner.run(createType("TestClass", false));
		// "Cannot load META-INF/slim-configuration-processor.properties (normal on first
		// full build)"
		assertThat(cr.getCompilationMessages()).extracting("message")
				.anyMatch(s -> ((String) s).contains("Cannot load"));
	}

	/**
	 * Generate a simple initializer for a class marked @Configuration
	 */
	@Test
	public void springConfigurationClass() {
		CompilationResult cr = CompilerRunner.run(createType("ConfigClass", true),
				getSpringDependencies());
		List<CompilationMessage> otherMessages = cr.getCompilationMessages().stream()
				.filter(cm -> cm.isOther()).collect(Collectors.toList());
		assertThat(otherMessages.get(1).getMessage())
				.isEqualTo("Found @Configuration in ConfigClass");
		assertThat(otherMessages.get(2).getMessage())
				.isEqualTo("Writing Initializer ConfigClassInitializer");
		assertThat(cr.containsNewFile("ConfigClassInitializer.class"));
		assertThat(
				cr.containsNewFile("META-INF/slim-configuration-processor.properties"));
		cr.printGeneratedSources(System.out);
		// TODO store these in getExpectedDataDirectory() for checking ?
		// assertThat(cr.getNewFileContents("ConfigClassInitializer.java")).isEqualTo("");
		System.out.println(">>>>\n" + cr.getGeneratedFileContents(
				"META-INF/slim-configuration-processor.properties"));
	}

	@Test
	public void incrementalBuild() throws IOException {
		Collection<InputFileDescriptor> inputs = new ArrayList<>();
		inputs.add(createType("ConfigClass2", true));
		inputs.add(createType("ConfigClass", true, "ConfigClass2"));
		CompilationResult cr = CompilerRunner.run(inputs, Collections.emptyList(),
				getSpringDependencies());
		assertContainsMessage(cr, "Found @Configuration in ConfigClass");
		assertContainsMessage(cr, "Found @Configuration in ConfigClass2");
		assertContainsMessage(cr, "Writing Initializer ConfigClassInitializer");
		assertContainsMessage(cr, "Writing Initializer ConfigClass2Initializer");
		Properties p = new Properties();
		String processorStateProperties = cr.getGeneratedFileContents(
				"META-INF/slim-configuration-processor.properties");
		p.load(new ByteArrayInputStream(processorStateProperties.getBytes()));
		assertThat(p.get("import.ConfigClass")).isEqualTo("ConfigClass2");
		cr.printGeneratedSources(System.out);
		// ... call compiler again for just the one file ...
		// ... check the state is loaded successfully
		// ... check the result is the same
	}

	@Test
	public void sampleConfigurationClass() {
		CompilationResult cr = CompilerRunner.run(
				new InputFileDescriptor(
						new File("src/test/java/"
								+ ClassUtils.classPackageAsResourcePath(getClass())
								+ "/SampleConfiguration.java"),
						"SampleConfiguration",
						ClassUtils.getPackageName(getClass()) + ".SampleConfiguration"),
				getSpringDependencies());
		List<CompilationMessage> otherMessages = cr.getCompilationMessages().stream()
				.filter(cm -> cm.isOther()).collect(Collectors.toList());
		assertThat(otherMessages.get(1).getMessage()).contains("Found @Configuration");
		assertThat(cr.containsNewFile("SampleConfigurationInitializer.class"));
		cr.printGeneratedSources(System.out);
	}

	@Test
	public void sampleApplicationClass() {
		CompilationResult cr = CompilerRunner.run(
				new InputFileDescriptor(
						new File("src/test/java/"
								+ ClassUtils.classPackageAsResourcePath(getClass())
								+ "/SampleApplication.java"),
						"SampleApplication",
						ClassUtils.getPackageName(getClass()) + ".SampleApplication"),
				getSpringDependencies());
		assertThat(cr.containsNewFile("SampleApplicationInitializer.class"));
		String generated = cr.getGeneratedFileContents(
				ClassUtils.classPackageAsResourcePath(getClass())
						+ "/SampleApplicationInitializer.java");
		assertThat(generated).contains("AutoConfigurationPackages.Registrar");
		assertThat(generated).contains("AutoConfigurationImportSelector");
		// bean methods come before imports
		assertThat(generated).containsSubsequence("Bar.class",
				"AutoConfigurationImportSelector");
	}

	// ---

	private void assertContainsMessage(CompilationResult cr, String expectedMessage) {
		boolean contains = cr.getCompilationMessages().stream()
				.anyMatch(cm -> cm.getMessage().equals(expectedMessage));
		if (!contains) {
			fail("Unable to find expected message '" + expectedMessage
					+ "' in compilation result: \n" + cr.getCompilationMessages());
		}
	}

	public File getExpectedDataDirectory() {
		String classname = this.getClass().getName();
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (int i = stack.length - 1; i >= 0; i--) {
			if (stack[i].getClassName().equals(classname)) {
				// First entry of our class in the stack
				int lastdot = classname.lastIndexOf(".");
				String simpleclassname = lastdot == -1 ? classname
						: classname.substring(lastdot + 1);
				return new File("./expected_data/" + simpleclassname + "/"
						+ stack[i].getMethodName());
			}
		}
		return null;
	}

	public static File resolveJunitConsoleLauncher() {
		DependencyResolver engine = DependencyResolver.instance();
		Artifact junitLauncherArtifact = new DefaultArtifact(
				"org.junit.platform:junit-platform-console-standalone:1.3.1");
		Dependency junitLauncherDependency = new Dependency(junitLauncherArtifact,
				"test");
		File file = engine.resolve(junitLauncherDependency);
		return file;
	}

	private List<File> getSpringDependencies() {
		try {
			File f = new File("pom.xml");
			DependencyResolver engine = DependencyResolver.instance();
			List<Dependency> dependencies = engine
					.dependencies(new FileUrlResource(f.toURI().toURL()));
			List<File> resolvedDependencies = dependencies.stream()
					.map(d -> engine.resolve(d)).collect(Collectors.toList());
			return resolvedDependencies;
		}
		catch (Exception e) {
			return null;
		}
	}

	public static final ClassName IMPORT = ClassName
			.get("org.springframework.context.annotation", "Import");
	public static final ClassName CONFIGURATION = ClassName
			.get("org.springframework.context.annotation", "Configuration");

	private static TypeSpec.Builder importAnnotation(TypeSpec.Builder type,
			String... fullyQualifiedImports) {
		ClassName[] array = new ClassName[fullyQualifiedImports.length];
		for (int i = 0; i < fullyQualifiedImports.length; i++) {
			array[i] = ClassName.bestGuess(fullyQualifiedImports[i]);
		}
		AnnotationSpec.Builder builder = AnnotationSpec.builder(IMPORT);
		builder.addMember("value",
				array.length > 1 ? ("{" + typeParams(array.length) + "}") : "$T.class",
				(Object[]) array);
		return type.addAnnotation(builder.build());
	}

	private static String typeParams(int count) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append("$T.class");
		}
		return builder.toString();
	}

	private static InputFileDescriptor createType(String classname,
			boolean markAtConfiguration, String... imports) {
		Builder builder = TypeSpec.classBuilder(classname);
		if (markAtConfiguration) {
			builder.addAnnotation(CONFIGURATION);
		}
		if (imports.length > 0) {
			importAnnotation(builder, imports);
		}
		// builder.addSuperinterface(SpringClassNames.INITIALIZER_TYPE);
		builder.addModifiers(Modifier.PUBLIC);
		// builder.addMethod(createInitializer());
		TypeSpec ts = builder.build();
		JavaFile file = JavaFile.builder("", ts).build();
		StringBuilder sb = new StringBuilder();
		try {
			file.writeTo(sb);
		}
		catch (IOException e) {
			throw new IllegalStateException(
					"Unable to write out source file: " + classname, e);
		}
		return new InputFileDescriptor(classname, sb.toString());
	}
}
