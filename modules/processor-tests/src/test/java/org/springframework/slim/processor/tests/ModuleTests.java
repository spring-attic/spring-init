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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.cloud.function.compiler.java.CompilationResult;
import org.springframework.cloud.function.compiler.java.InputFileDescriptor;
import org.springframework.slim.processor.infra.CompilerRunner;
import org.springframework.slim.processor.infra.Utils;

/**
 * Find the *Tests.java in the module/tests/src/test/java folder, pull together the
 * related sources and compile and run the test.
 * 
 * @author Andy Clement
 */
@EnabledIfSystemProperty(named = "generatedTests", matches = "true")
public class ModuleTests {

	private static File moduleTestsFolder = new File("../../tests/tests");

	@TestFactory
	public Stream<DynamicTest> moduleTests() {
		return findExistingTests().stream()
				.map(f -> DynamicTest.dynamicTest(f.getClassName(), () -> runTest(f)));
	}

	/**
	 * @return files matching: module/tests/src/test/java/*Tests.java
	 */
	List<InputFileDescriptor> findExistingTests() {
		return Utils.getFiles(new File(moduleTestsFolder, "src/test/java")).stream()
				.filter(f -> f.getClassName().endsWith("Tests"))
				.collect(Collectors.toList());
	}

	private static void runTest(InputFileDescriptor testSourceFile) {
		Set<InputFileDescriptor> inputForCompiler = new HashSet<>();
		inferOtherSources(testSourceFile, inputForCompiler);
		Set<InputFileDescriptor> resources = new HashSet<>();
		resources.addAll(
				Utils.getFiles(new File(moduleTestsFolder, "src/main/resources")));
		resources.addAll(
				Utils.getFiles(new File(moduleTestsFolder, "src/test/resources")));
		List<File> dependencies = new ArrayList<>(
				resolveModuleTestsProjectDependencies());
		System.out.println("For test " + testSourceFile.getClassName());
		System.out.println(" - compiling #" + inputForCompiler.size() + " sources");
		System.out.println(" - resources #" + resources.size() + " files");
		System.out.println(" - dependencies #" + dependencies.size());
		CompilationResult result = CompilerRunner.run(inputForCompiler, resources,
				dependencies);
		dependencies.add(0, result.dumpToTemporaryJar());
		Utils.executeTests(result, dependencies, testSourceFile);
	}

	private static List<File> resolveModuleTestsProjectDependencies() {
		return Utils.resolveProjectDependencies(moduleTestsFolder);
	}

	private static void inferOtherSources(InputFileDescriptor testclass,
			Set<InputFileDescriptor> collector) {
		Set<String> importedPackages = Utils.findImports(testclass);
		String thisPkg = testclass.getPackageName();
		collector.add(testclass);
		// Find other Java sources in those packages that we should include
		List<InputFileDescriptor> mainJavaSources = Utils
				.getFiles(new File(moduleTestsFolder, "src/main/java")).stream()
				.filter(f -> importedPackages.contains(f.getPackageName()))
				.collect(Collectors.toList());
		for (InputFileDescriptor f : mainJavaSources) {
			if (collector.add(f) && !f.getPackageName().equals(thisPkg)) {
				inferOtherSources(f, collector);
			}
		}
		List<InputFileDescriptor> testJavaSources = Utils
				.getFiles(new File(moduleTestsFolder, "src/test/java")).stream()
				.filter(f -> importedPackages.contains(f.getPackageName()))
				.collect(Collectors.toList());
		for (InputFileDescriptor f : testJavaSources) {
			if (collector.add(f) && !f.getPackageName().equals(thisPkg)) {
				inferOtherSources(f, collector);
			}
		}
	}

}